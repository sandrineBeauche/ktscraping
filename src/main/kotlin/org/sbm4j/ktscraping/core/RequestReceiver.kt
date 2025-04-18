package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.ErrorLevel
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status
import kotlin.coroutines.CoroutineContext


/**
 * An object from the kt scraping line that can receive and process requests.
 * This object is fully asynchronous, so the reception of requests are in a new coroutine
 * @property mutex
 * @property requestIn the channel used to receive the requests
 * @property responseOut the channel used to send the responses from the requests
 */
interface RequestReceiver: Controllable{

    var requestIn: ReceiveChannel<AbstractRequest>

    val responseOut: SendChannel<Response>

    /**
     * Receives all the requests and process them.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun performRequests(){
        scope.launch(CoroutineName("${name}-performRequests")) {
            for(req in requestIn){
                this.launch() {
                    try {
                        logger.trace { "${name}: received request ${req.name}" }
                        var result: Any? = processRequest(req)

                        if ((result is Boolean && result == true) || result != null) {
                            answerRequest(req, result)
                        }
                    }
                    catch(ex: Exception){
                        logger.error{ "${this@RequestReceiver.name}: error when processing request ${req.name} - ${ex.message}" }
                        val response = Response(req, Status.ERROR)
                        response.contents["level"] = ErrorLevel.MAJOR
                        response.contents["exception"] = ex
                        response.contents["controllableName"] = this@RequestReceiver.name
                        responseOut.send(response)
                    }
                }
                logger.trace { "${name}: ready to receive another request" }
            }
            logger.debug{"${name}: Finished to receive requests"}
        }
    }

    /**
     * Performs some treatments on the requests
     * @param request the request to be processes
     * @return the result of the request. It is a response for a request receiver that produces a response, or true if the request should follow. It returns null or false if the request should be ignored.
     */
    suspend fun processRequest(request: AbstractRequest): Any?

    /**
     * Answers the request. The answer from a request could be following the requests to the next object,
     * or send back a response.
     * @param request the request to be answered
     * @param result the result of the request processing
     */
    suspend fun answerRequest(request: AbstractRequest, result: Any)


    override suspend fun run() {
        this.performRequests()
    }

    override suspend fun stop() {
        this.responseOut.close()
    }


}