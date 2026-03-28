package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.dispatchers.Router
import org.sbm4j.meercat.nodes.logger

abstract class DownloaderDispatcher(
    override val name: String = "DownloaderDispatcher",
    override val di: DI
): EventDispatcher, Router, AbstractComponent(){

    override val channelOuts: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelIn: SuperChannel


    abstract fun selectChannel(request: AbstractRequest): SuperChannel

    suspend fun performRequests(){
        val flow = channelIn.getSendFlow(AbstractRequest::class)
        val coroutineName = "${name}-performRequests"
        route(coroutineName, flow, ::selectChannel)
        forwardBacks { it is AbstractRequest }
    }

    override suspend fun run() {
        performRequests()
        super<EventDispatcher>.run()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the downloader dispatcher ${name}"}
        super<EventDispatcher>.stop()
        super<AbstractComponent>.stop()
    }
}