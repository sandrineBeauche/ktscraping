package org.sbm4j.ktscraping.core

import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest

/**
 * An object from the kt scraping line that can receive and process requests.
 * This object is fully asynchronous, so the reception of requests are in a new coroutine
 * @property mutex
 * @property requestIn the channel used to receive the requests
 * @property responseOut the channel used to send the responses from the requests
 */
interface RequestReceiver: EventConsumer{


    suspend fun processSend(send: AbstractRequest): Any? {
        return when(send) {
            is EventRequest -> consumeEvent(send)
            is DownloadingRequest -> processDataRequest(send)
            else -> null
        }
    }


    /**
     * Performs some treatments on the requests for downloading data
     * @param request the request to be processes
     * @return the result of the request. It is a response for a request receiver that produces a response, or true if the request should follow. It returns null or false if the request should be ignored.
     */
    suspend fun processDataRequest(request: DownloadingRequest): Any?

}