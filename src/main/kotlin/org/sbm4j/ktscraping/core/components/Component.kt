package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.core.processors.EventSink
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.AbstractProcessingNode
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.Node
import org.sbm4j.meercat.nodes.sendProcessors.AbstractInitiator
import java.util.concurrent.ConcurrentHashMap


/**
 * A thread-safe mutable state map for [Component] nodes.
 *
 * Used to store any data that needs to persist across messages within a component
 * (e.g. counters, open connections, local cache, session data).
 */
typealias  State = ConcurrentHashMap<String, Any>

/**
 * Base interface for all KtScraping nodes in the topology.
 *
 * Extends Meercat's [Node] with a [state] map for stateful processing and lifecycle
 * hooks ([pause], [resume]) for future scraping session management.
 *
 * All three branches of the topology (Spider, Downloader, Pipeline) are composed
 * of [Component] nodes.
 *
 * @see State
 * @see AbstractComponent
 */
interface Component: Node {

    /**
     * Mutable state of this component, persisted across messages.
     *
     * Can hold any data relevant to the component's processing (e.g. counters,
     * open connections, local cache, session data).
     */
    var state: State

    /**
     * Pauses this component's processing.
     *
     * Reserved for future lifecycle management. Default implementation is a no-op.
     */
    suspend fun pause(){
    }

    /**
     * Resumes this component's processing after a [pause].
     *
     * Reserved for future lifecycle management. Default implementation is a no-op.
     */
    suspend fun resume(){
    }
}

/**
 * Base abstract class for generic processing [Component] nodes.
 *
 * Provides a default [State] implementation for nodes based on [AbstractProcessingNode].
 *
 * @see Component
 */
abstract class AbstractComponent(): Component, AbstractProcessingNode(){
    override var state: State = State()
}

/**
 * Base abstract class for [Initiator] components in the Spider branch.
 *
 * Spiders extend this class to drive the scraping session by emitting [Event],
 * [AbstractRequest] and [Item] messages. Provides a default [State] implementation.
 *
 * @see Component
 * @see AbstractInitiator
 */
abstract class AbstractInitiatorComponent: AbstractInitiator(), Component{
    override var state: State = State()
}

/**
 * Base abstract class for intermediate [Component] nodes in the topology.
 *
 * Combines Meercat's [AbstractMiddleNode] with [EventConsumer] and [EventBackForwarder]
 * to handle the full event lifecycle: receiving events from upstream, launching
 * pre-event jobs, and processing backs with post-event hooks before forwarding upstream.
 *
 * On startup, [run] concurrently registers event listening, event back listening,
 * and the standard middle node processing.
 *
 * @param name The name of this component node.
 *
 * @see EventConsumer
 * @see EventBackForwarder
 * @see AbstractMiddleNode
 */
abstract class AbstractMiddleComponent(
    name: String
): AbstractMiddleNode(name), Component, EventConsumer, EventBackForwarder{

    override var state: State = State()

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
        = ConcurrentHashMap()

    override suspend fun run() {
        super<EventConsumer>.run()
        super<EventBackForwarder>.run()
        super<AbstractMiddleNode>.run()
    }
}

/**
 * Base abstract class for terminal (sink) [Component] nodes in the topology.
 *
 * Combines Meercat's [AbstractSinkNode] with [EventSink] to handle events at the end
 * of a branch: the pre-event job is awaited immediately and the [EventBack] is built
 * and returned without forwarding downstream.
 *
 * On startup, [run] concurrently registers event sink listening and the standard
 * sink node processing.
 *
 * @param name The name of this component node.
 *
 * @see EventSink
 * @see AbstractSinkNode
 */
abstract class AbstractSinkComponent(
    name: String
): AbstractSinkNode(name), Component, EventSink {
    override var state: State = State()

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
        = ConcurrentHashMap()

    override suspend fun run() {
        super<EventSink>.run()
        super<AbstractSinkNode>.run()
    }
}
