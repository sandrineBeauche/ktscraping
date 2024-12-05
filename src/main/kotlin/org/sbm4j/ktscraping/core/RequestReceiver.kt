package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response


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
    suspend fun performRequests(){
        scope.launch(CoroutineName("${name}-performRequests")) {
            for(req in requestIn){
                logger.debug{ "received request ${req.name} on ${name}"}
                var result: Any? = null
                mutex.withLock {
                    result = processRequest(req)
                }

                answerRequest(req, result)
            }
        }
    }

    /**
     * Performs some treatments on the requests
     * @param request the request to be processes
     * @return true if the request should be answered, false otherwise
     */
    fun processRequest(request: AbstractRequest): Any?

    /**
     * Answers the request. The answer from a request could be following the requests to the next object,
     * or send back a response.
     * @param request the request to be answered
     * @param result the result of the request processing
     */
    suspend fun answerRequest(request: AbstractRequest, result: Any?)

    override suspend fun start() {
        this.performRequests()
    }

    override suspend fun stop() {
        this.responseOut.close()
    }


}