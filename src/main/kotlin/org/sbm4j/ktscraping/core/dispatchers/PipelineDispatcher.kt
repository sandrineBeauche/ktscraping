package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractComponent

import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.nodes.dispatchers.Broadcast
import org.sbm4j.meercat.nodes.dispatchers.Router
import org.sbm4j.meercat.nodes.logger

abstract class PipelineDispatcher(
    override val name: String,
    override val di: DI
) : EventDispatcher, AbstractComponent(), DIAware {

    override val channelOuts: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelIn: SuperChannel

    abstract suspend fun performItems()

    override suspend fun run() {
        performItems()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the pipeline dispatcher ${name}"}
        super<EventDispatcher>.stop()
        super<AbstractComponent>.stop()
    }

}

class PipelineDispatcherAll(name: String, di: DI): PipelineDispatcher(name, di), Broadcast {

    override suspend fun performItems(){
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        broadcast(coroutineName, flow)
    }

}

abstract class PipelineDispatcherOne(name: String, di: DI): PipelineDispatcher(name, di), Router {

    abstract fun selectChannel(item: Item): SuperChannel

    override suspend fun performItems() {
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        //route(coroutineName, flow, ::selectChannel)
        //forwardBacks { it is Item }
    }
}