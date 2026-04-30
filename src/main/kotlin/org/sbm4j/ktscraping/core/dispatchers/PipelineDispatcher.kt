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

/**
 * Base abstract class for dispatching messages in the Pipeline branch.
 *
 * [PipelineDispatcher] serves a dual role in the topology:
 * - As an [EventDispatcher]: broadcasts [Event] messages to all registered Pipeline branches.
 * - As a dispatcher for [Item] messages: concrete subclasses define the dispatch strategy
 *   via [PipelineDispatcherAll] (broadcast to all exporters) or [PipelineDispatcherOne]
 *   (route to a single exporter).
 *
 * @param name The name of this dispatcher node.
 * @param di The Kodein dependency injection container, used for topology construction.
 *
 * @see PipelineDispatcherAll
 * @see PipelineDispatcherOne
 * @see EventDispatcher
 * @see AbstractPropagator
 */
abstract class PipelineDispatcher(
    override val name: String,
    override val di: DI
) : EventDispatcher, AbstractPropagator(), DIAware {

    override var state: State = State()

    override suspend fun run() {
        super<EventDispatcher>.run()
        super<AbstractPropagator>.run()
    }

    override suspend fun stop() {
        logger.info{ "${name}: Stopping the pipeline dispatcher"}
        super<AbstractPropagator>.stop()
    }

}

/**
 * A [PipelineDispatcher] that broadcasts every [Item] to all registered Pipeline branches.
 *
 * Use this when scraped items must be processed by all exporters simultaneously
 * (e.g. exporting to a database AND writing to a CSV file at the same time).
 * Meercat's [Broadcast] mechanism sends the item to all branches and aggregates
 * the [ItemAck] backs before returning to the Spider.
 *
 * @param name The name of this dispatcher node.
 * @param di The Kodein dependency injection container, used for topology construction.
 *
 * @see PipelineDispatcher
 * @see PipelineDispatcherOne
 */
class PipelineDispatcherAll(name: String, di: DI):
    PipelineDispatcher(name, di),
    Broadcast
{

    override suspend fun run() {
        logger.info{ "${name}: Starting the pipeline dispatcher all"}
        val flow = channelIn.getSendFlow(Item::class)
        val coroutineName = "${name}-performItems"
        broadcast(coroutineName, flow)
        super<PipelineDispatcher>.run()
    }

}

/**
 * A [PipelineDispatcher] that routes each [Item] to a single Pipeline branch.
 *
 * Use this when different item types must be routed to different exporters
 * (e.g. routing [ObjectDataItem] of type `Product` to a product exporter and
 * `Article` to an article exporter). Concrete subclasses implement [selectChannel]
 * to define the routing logic.
 *
 * @param name The name of this dispatcher node.
 * @param di The Kodein dependency injection container, used for topology construction.
 *
 * @see PipelineDispatcher
 * @see PipelineDispatcherAll
 */
abstract class PipelineDispatcherOne(name: String, di: DI): PipelineDispatcher(name, di), Router {

    abstract fun selectChannel(item: Item): SuperChannel

    override suspend fun run() {
        logger.info{ "${name}: Stopping the pipeline dispatcher one"}
        performSendBacks(Item::class, ItemAck::class,
            null, ::selectChannel)
        super<PipelineDispatcher>.run()
    }
}