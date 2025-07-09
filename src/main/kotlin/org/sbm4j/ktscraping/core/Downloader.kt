package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import java.util.concurrent.ConcurrentHashMap
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.ktscraping.data.response.Status

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
): RequestReceiver{

    companion object{
        val PAYLOAD: String = "payload"
        val FRAMES: String = "frames"
        val CONTENT_TYPE: String = "contentType"
    }

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var requestIn: ReceiveChannel<AbstractRequest>
    override lateinit var responseOut: SendChannel<Response<*>>

    override lateinit var scope: CoroutineScope

    override val pendingEvent: ConcurrentHashMap<String, Job> = ConcurrentHashMap()

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        logger.debug { "$name : answer to ${request.name} with a response"}
        responseOut.send(result as Response<*>)
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
            return EventResponse(event as EventRequest, Status.ERROR, infos)
        }
    }


    override suspend fun run() {
        logger.info{"${name}: Starting downloader"}
        super.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super.stop()
    }
}