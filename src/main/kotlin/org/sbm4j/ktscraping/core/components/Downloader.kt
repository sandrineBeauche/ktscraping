package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.RequestReceiver
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack

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
): AbstractControllable(), RequestReceiver, EventConsumer {

    companion object{
        val PAYLOAD: String = "payload"
        val FRAMES: String = "frames"
        val CONTENT_TYPE: String = "contentType"
    }


    override lateinit var inChannel: SuperChannel

    override suspend fun consumeEvent(event: Event): Any? {
        lateinit var result: EventBack
        try {
            performEvent(event)
            result = event.buildBack()
        }
        catch(ex: Exception){
            val error = this.generateErrorInfos(ex)
            result = event.buildErrorBack(error)
        }
        finally {
            performPostEvent(result)
        }
        return result
    }


    override suspend fun sendPostProcess(send: Send, result: Any) {
        if(result is Back<*>) {
            logger.debug { "$name : answer to ${send.loggingLabel} ${send.name} with a back"}
            inChannel.send(result)
        }
    }


    override suspend fun run() {
        logger.info{"${name}: Starting downloader"}
        super<EventConsumer>.run()
        super<RequestReceiver>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super<RequestReceiver>.stop()
        super<EventConsumer>.stop()
    }
}