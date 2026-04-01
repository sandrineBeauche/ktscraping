package org.sbm4j.ktscraping.core.components

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventSink
import org.sbm4j.ktscraping.core.processors.RequestReceiver
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.logger

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
    name: String
): AbstractSinkComponent(name), RequestReceiver, EventSink {

    companion object{
        val PAYLOAD: String = "payload"
        val FRAMES: String = "frames"
        val CONTENT_TYPE: String = "contentType"
    }

    override suspend fun run() {
        logger.info{"${name}: Starting downloader"}
        super<RequestReceiver>.run()
        super<AbstractSinkComponent>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super<AbstractSinkComponent>.stop()
    }
}