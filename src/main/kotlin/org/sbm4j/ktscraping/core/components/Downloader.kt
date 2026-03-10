package org.sbm4j.ktscraping.core.components

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.EventSink
import org.sbm4j.ktscraping.core.processors.RequestReceiver
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Send
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.logger

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
): AbstractControllable(), RequestReceiver, EventSink {

    companion object{
        val PAYLOAD: String = "payload"
        val FRAMES: String = "frames"
        val CONTENT_TYPE: String = "contentType"
    }


    override lateinit var inChannel: SuperChannel


    override suspend fun sendPostProcess(send: Send, result: Any) {
        if(result is Back<*>) {
            logger.debug { "$name : answer to ${send.loggingLabel} ${send.name} with a back"}
            inChannel.send(result)
        }
    }


    override suspend fun run() {
        logger.info{"${name}: Starting downloader"}
        super<EventSink>.run()
        super<RequestReceiver>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super<RequestReceiver>.stop()
        super<EventSink>.stop()
    }
}