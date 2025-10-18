package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.item.Item

abstract class PipelineDispatcher(
    override val name: String,
    override val di: DI
) : EventDispatcher, AbstractControllable(), DIAware {

    override val senders: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelIn: SuperChannel

    abstract suspend fun performItems()

    override suspend fun run() {
        super.run()
        performItems()
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