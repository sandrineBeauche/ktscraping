package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.data.item.Item

abstract class PipelineDispatcher(
    override val name: String,
    override val di: DI
) : EventDispatcher, AbstractControllable(), DIAware {

    override val receivers: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelIn: SuperChannel

    abstract suspend fun performItems()

    override suspend fun run() {
        performItems()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the pipeline dispatcher ${name}"}
        super<EventDispatcher>.stop()
        super<AbstractControllable>.stop()
    }

}

class PipelineDispatcherAll(name: String, di: DI): PipelineDispatcher(name, di), SendPropagatorAll{

    override suspend fun performItems(){
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        propagateAll(coroutineName, flow)
    }

}

abstract class PipelineDispatcherOne(name: String, di: DI): PipelineDispatcher(name, di), SendPropagatorOne{

    abstract fun selectChannel(item: Item): SuperChannel

    override suspend fun performItems() {
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        propagateOne(coroutineName, flow, ::selectChannel)
    }
}