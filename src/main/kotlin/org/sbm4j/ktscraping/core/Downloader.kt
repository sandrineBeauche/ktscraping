package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

abstract class AbstractDownloader(override val scope: CoroutineScope,
                          override val name: String): RequestReceiver{

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var requestIn: ReceiveChannel<Request>
    override lateinit var responseOut: SendChannel<Response>


    override suspend fun answerRequest(request: Request, result: Any?) {
        responseOut.send(result as Response)
    }


    override suspend fun start() {
        logger.info{"Starting downloader ${name}"}
        super.start()
    }

    override suspend fun stop() {
        logger.info{"Stopping spider ${name}"}
        super.stop()
    }
}