package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.RequestReceiver
import org.sbm4j.meercat.nodes.logger

enum class ContentType{
    HTML,
    XML,
    JSON,
    SVG_IMAGE,
    BITMAP_IMAGE,
    IMAGE,
    FILE,
    STRING,
    NOTHING
}

abstract class AbstractDownloader(
    name: String
): AbstractSinkComponent(name), RequestReceiver {

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