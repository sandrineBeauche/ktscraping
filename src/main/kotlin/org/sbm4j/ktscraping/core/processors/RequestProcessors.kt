package org.sbm4j.ktscraping.core.processors

import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

typealias CallbackError = suspend (Throwable) -> Unit


class NoRequestSenderException(message: String) : Exception(message)



/**
 * A component from the kt scraping line that can send requests and receive responses.
 * The object is fully asynchronous, so the requests are sent in a coroutine while the
 * responses are received in another coroutine.
 * @property mutex a mutex that allows to safely executes callbacks and process response as they can modify the shared state
 * @property pendingRequests the request that wait for a response
 * @property requestOut the channel used to send the requests
 * @property responseIn the channel used to receive the responses
 * @author Sandrine Ben Mabrouk
 */
interface  RequestSender: SendSource {


}


interface ResponseForwarder: BackForwarder {
    /**
     * Processes a response
     * @param response the response to be processed
     * @return true if the response should be followed to the previous piece, false otherwise.
     */
    suspend fun processResponse(response: Response)

    override suspend fun run() {
        val flow = this@ResponseForwarder.outChannel.getBackFlow<Response>(Response::class, this)
        receiveBacks(Response::class, flow, ::processResponse)
    }
}

/**
 * An object from the kt scraping line that can receive and process requests.
 * This object is fully asynchronous, so the reception of requests are in a new coroutine
 * @property mutex
 * @property requestIn the channel used to receive the requests
 * @property responseOut the channel used to send the responses from the requests
 */
interface RequestReceiver: SendConsumer {


    /**
     * Performs some treatments on the requests for downloading data
     * @param request the request to be processes
     * @return the result of the request. It is a response for a request receiver that produces a response, or true if the request should follow. It returns null or false if the request should be ignored.
     */
    suspend fun processRequest(request: AbstractRequest): Any?{
        return when(request){
            is DownloadingRequest -> processDataRequest(request)
            else -> true
        }
    }

    suspend fun processDataRequest(request: DownloadingRequest): Any?{
        return null
    }

    override suspend fun run() {
        logger.debug{"run from Request processor"}
        val clazz = AbstractRequest::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::processRequest)
        logger.debug{"done run from Request processor"}
    }
}

interface RequestForwarder: RequestReceiver, SendForwarder {
    override suspend fun run() {
        super<RequestReceiver>.run()
        super<SendForwarder>.run()
    }
}
