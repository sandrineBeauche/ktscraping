package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.components.State
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.dispatchers.AbstractPropagator
import org.sbm4j.meercat.nodes.dispatchers.Router
import org.sbm4j.meercat.nodes.logger

abstract class DownloaderDispatcher(
    override val name: String = "DownloaderDispatcher",
    override val di: DI
): EventDispatcher, Router, AbstractPropagator() {

    override var state: State = State()

    abstract fun selectChannel(request: AbstractRequest): SuperChannel


    override suspend fun run() {
        logger.info{ "${name}: Starting the downloader dispatcher"}
        performSendBacks(AbstractRequest::class, Response::class,
            null, ::selectChannel)
        super<EventDispatcher>.run()
        super<AbstractPropagator>.run()
    }

    override suspend fun stop() {
        logger.info{ "${name}: Stopping the downloader dispatcher"}
        super<AbstractPropagator>.stop()
    }
}