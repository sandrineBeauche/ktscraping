package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * A piece of the kt scraping line to perform some operations on requests, and answer by
 * following them to the next piece. In the other side, Middleware processes responses
 * before following them to the previous piece.
 */
interface Middleware : RequestSender, RequestReceiver {


    /**
     * Answers a request by following it to the next piece or return a response to the sender
     */
    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        if(result is DownloadingResponse){
            logger.trace { "${name}: returns a response for the request ${request.name}" }
            responseOut.send(result)
        }
        else {
            logger.trace { "${name}: forward request ${request.name}" }
            requestOut.send(request)
        }
    }

    override suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        if(response.request.sender != this){
            var result = true
            try {
                result = processResponse(response, response.request)
            }
            catch(ex: Exception){
                val infos = ErrorInfo(ex, this, ErrorLevel.MAJOR)
                response.status = Status.ERROR
                response.errorInfos.add(infos)
            }
            if(result){
                logger.trace{"${name}: forward response from the request ${response.request.name}"}
                responseOut.send(response)
            }
        }
        else{
            throw NoRequestSenderException("No request found for this sender")
        }
    }

    /**
     * Processes a response
     * @param response the response to be processed
     * @return true if the response should be followed to the previous piece, false otherwise.
     */
    suspend fun processResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean



    override suspend fun resumeEvent(event: EventBack) {
        super<RequestReceiver>.resumeEvent(event)
        responseOut.send(event as EventResponse)
    }

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return ErrorInfo(ex, this, ErrorLevel.MAJOR)
    }

    override suspend fun run() {
        super<RequestReceiver>.run()
        super<RequestSender>.run()
    }

    override suspend fun stop() {
        super<RequestReceiver>.stop()
        super<RequestSender>.stop()
    }
}


abstract class AbstractMiddleware(override val name: String): Middleware{
    override val mutex: Mutex = Mutex()
    override var state: State = State()
    override var pendingRequests: PendingRequestMap = PendingRequestMap()

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override lateinit var requestIn: ReceiveChannel<AbstractRequest>
    override lateinit var responseOut: SendChannel<Response<*>>

    override lateinit var requestOut: SendChannel<AbstractRequest>
    override lateinit var responseIn: ReceiveChannel<Response<*>>

    override lateinit var scope: CoroutineScope

    override val pendingMinorError: ConcurrentHashMap<Int, MutableList<ErrorInfo>> =
        ConcurrentHashMap()

}
/**
 *
 */
abstract class SpiderMiddleware(name:String):
    AbstractMiddleware(name), ItemForwarder {

    override lateinit var itemIn: ReceiveChannel<Item>
    override lateinit var itemOut: SendChannel<Item>

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        super<AbstractMiddleware>.run()
        super<ItemForwarder>.run()
    }

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return super<AbstractMiddleware>.generateErrorInfos(ex)
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping spider middleware"}
        super<AbstractMiddleware>.stop()
        super<ItemForwarder>.stop()
    }

}

abstract class DownloaderMiddleware(name: String): AbstractMiddleware(name){

    override suspend fun stop() {
        super.stop()
    }
}
