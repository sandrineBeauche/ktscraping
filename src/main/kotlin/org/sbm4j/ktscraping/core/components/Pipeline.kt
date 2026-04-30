package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.*
import org.sbm4j.meercat.nodes.logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for intermediate nodes in the Pipeline branch.
 *
 * A [Pipeline] node sits between the Spider and the [AbstractExporter] in the Pipeline
 * branch. It receives [Item] messages (via [ItemForwarder]), processes them (e.g.
 * transformation, enrichment, validation, filtering), and forwards [ItemAck] backs
 * toward the Spider (via [ItemAckForwarder]).
 *
 * Multiple [Pipeline] nodes can be chained to form a processing pipeline before
 * items reach the terminal [AbstractExporter].
 *
 * @see ItemForwarder
 * @see ItemAckForwarder
 * @see AbstractPipeline
 * @see AbstractExporter
 */
interface Pipeline : ItemForwarder, ItemAckForwarder{

    override suspend fun run() {
        logger.info { "${name}: Starting pipeline" }
        super<ItemForwarder>.run()
        super<ItemAckForwarder>.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: Stopping pipeline" }
        super<ItemForwarder>.stop()
        super<ItemAckForwarder>.stop()
    }

}

/**
 * Base abstract class for intermediate Pipeline branch nodes.
 *
 * Combines [Pipeline] with [AbstractMiddleComponent] to provide a full-featured
 * intermediate pipeline node: item processing (via [Pipeline]) and event lifecycle
 * management (via [AbstractMiddleComponent] — [EventConsumer] and [EventBackForwarder]).
 *
 * Concrete subclasses override [ItemReceiver.processItem] to implement their
 * specific transformation, enrichment, validation, or filtering logic.
 *
 * @param name The name of this pipeline node.
 *
 * @see Pipeline
 * @see AbstractMiddleComponent
 * @see AbstractExporter
 */
abstract class AbstractPipeline(name: String) : Pipeline, AbstractMiddleComponent(name) {

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
            = ConcurrentHashMap()

    override suspend fun run() {
        super<Pipeline>.run()
        super<AbstractMiddleComponent>.run()
    }
}