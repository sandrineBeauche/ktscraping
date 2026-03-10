package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.data.request.AbstractRequest

abstract class DownloaderDispatcher(
    override val name: String = "DownloaderDispatcher",
    override val di: DI
): EventDispatcher, SendPropagatorOne, AbstractControllable(){

    override val receivers: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelIn: SuperChannel


    abstract fun selectChannel(request: AbstractRequest): SuperChannel

    suspend fun performRequests(){
        val flow = channelIn.getSendFlow(AbstractRequest::class)
        val coroutineName = "${name}-performRequests"
        propagateOne(coroutineName, flow, ::selectChannel)
    }

    override suspend fun run() {
        performRequests()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the downloader dispatcher ${name}"}
        super<EventDispatcher>.stop()
        super<AbstractControllable>.stop()
    }
}