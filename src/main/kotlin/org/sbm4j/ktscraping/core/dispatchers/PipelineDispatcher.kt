package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.components.State

import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.dispatchers.AbstractPropagator
import org.sbm4j.meercat.nodes.dispatchers.Broadcast
import org.sbm4j.meercat.nodes.dispatchers.Router
import org.sbm4j.meercat.nodes.logger

abstract class PipelineDispatcher(
    override val name: String,
    override val di: DI
) : EventDispatcher, AbstractPropagator(), DIAware {

    override var state: State = State()

    override suspend fun stop() {
        logger.info{ "${name}: Stopping the pipeline dispatcher"}
        super<AbstractPropagator>.stop()
    }

}

class PipelineDispatcherAll(name: String, di: DI): PipelineDispatcher(name, di), Broadcast {

    override suspend fun run() {
        logger.info{ "${name}: Starting the pipeline dispatcher all"}
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        broadcast(coroutineName, flow)
    }

}

abstract class PipelineDispatcherOne(name: String, di: DI): PipelineDispatcher(name, di), Router {

    abstract fun selectChannel(item: Item): SuperChannel

    override suspend fun run() {
        logger.info{ "${name}: Stopping the pipeline dispatcher one"}
        performSendBacks(Item::class, ItemAck::class,
            null, ::selectChannel)
    }
}