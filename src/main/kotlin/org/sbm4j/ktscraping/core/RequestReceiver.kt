package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.ktscraping.data.response.Status


/**
 * An object from the kt scraping line that can receive and process requests.
 * This object is fully asynchronous, so the reception of requests are in a new coroutine
 * @property mutex
 * @property requestIn the channel used to receive the requests
 * @property responseOut the channel used to send the responses from the requests
 */
interface RequestReceiver: Controllable, EventConsumer{

    var requestIn: ReceiveChannel<AbstractRequest>

    val responseOut: SendChannel<Response<*>>

    /**
     * Receives all the requests and process them.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun performRequests(){
        scope.launch(CoroutineName("${name}-performRequests")) {
            logger.debug { "${name}: Waits for requests to process" }
            for(req in requestIn){
                this.launch() {
                    try {
                        logger.trace { "${name}: received request ${req.name}" }
                        val result: Any? = if(req is EventRequest) {
                            consumeEvent(req)
                        }
                        else{
                            processDataRequest(req as DownloadingRequest)
                        }

                        if ((result is Boolean && result) || result != null) {
                            answerRequest(req, result)
                        }
                    }
                    catch(ex: Exception){
                        logger.error{ "${this@RequestReceiver.name}: error when processing request ${req.name} - ${ex.message}" }
                        val response = buildErrorResponse(req, ex)
                        responseOut.send(response)
                    }
                }
                logger.trace { "${name}: ready to receive another request" }
            }
            logger.debug{"${name}: Finished to receive requests"}
        }
    }

    fun buildErrorResponse(request: AbstractRequest, ex: Exception): Response<*>{
        val infos = ErrorInfo(ex, this@RequestReceiver, ErrorLevel.MAJOR)
        val result =  when(request){
            is DownloadingRequest -> {
                val response = DownloadingResponse(request, status = Status.ERROR)
                response.contents["level"] = ErrorLevel.MAJOR
                response.contents["exception"] = ex
                response.contents["controllableName"] = this@RequestReceiver.name
                response
            }
            is EventRequest -> {
                EventResponse(request, Status.ERROR)
            }
            else -> {
               null
            }
        }
        return result!!
    }

    /**
     * Performs some treatments on the requests for downloading data
     * @param request the request to be processes
     * @return the result of the request. It is a response for a request receiver that produces a response, or true if the request should follow. It returns null or false if the request should be ignored.
     */
    suspend fun processDataRequest(request: DownloadingRequest): Any?


    /**
     * Answers the request. The answer from a request could be forwarding the requests to the next component,
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