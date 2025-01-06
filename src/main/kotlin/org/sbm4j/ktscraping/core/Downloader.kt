package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

abstract class AbstractDownloader(override val scope: CoroutineScope,
                          override val name: String): RequestReceiver{

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var requestIn: ReceiveChannel<AbstractRequest>
    override lateinit var responseOut: SendChannel<Response>


    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        logger.debug { "$name : answer to ${request.name} with a response"}
        responseOut.send(result as Response)
    }


    override suspend fun start() {
        logger.info{"${name}: Starting downloader"}
        super.start()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super.stop()
    }
}