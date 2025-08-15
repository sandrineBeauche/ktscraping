package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import java.util.concurrent.ConcurrentHashMap

enum class ContentType{
    HTML,
    XML,
    JSON,
    SVG_IMAGE,
    BITMAP_IMAGE,
    IMAGE,
    FILE,
    NOTHING
}

abstract class AbstractDownloader(
    override val name: String
): Controllable, RequestReceiver{

    companion object{
        val PAYLOAD: String = "payload"
        val FRAMES: String = "frames"
        val CONTENT_TYPE: String = "contentType"
    }

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    lateinit var inChannel: SuperChannel


    override lateinit var scope: CoroutineScope

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()


    suspend fun answerRequest(request: AbstractRequest, result: Any) {
        logger.debug { "$name : answer to request ${request.name} with a response"}
        inChannel.send(result as Response<*>)
    }

    override suspend fun consumeEvent(event: Event): Any? {
        try {
            super.consumeEvent(event)
            val response = EventResponse(event as EventRequest)
            resumeEvent(response)
            return response
        }
        catch(ex: Exception){
            val infos = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            return EventResponse(event as EventRequest,
                Status.ERROR, mutableListOf(infos))
        }
    }

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return ErrorInfo(ex, this, ErrorLevel.MAJOR)
    }


    override suspend fun run() {
        logger.info{"${name}: Starting downloader"
    }

    suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super.stop()
    }
}}