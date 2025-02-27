package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response

enum class ContentType{
    HTML,
    XML,
    JSON,
    SVG_IMAGE,
    BITMAP_IMAGE,
    IMAGE,
    FILE
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
    override lateinit var responseOut: SendChannel<Response>

    override lateinit var scope: CoroutineScope


    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        logger.debug { "$name : answer to ${request.name} with a response"}
        responseOut.send(result as Response)
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