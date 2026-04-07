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
 *
 */
typealias  State = ConcurrentHashMap<String, Any>

/**
 * A component of the kt scraping crawler that can be started, stopped or paused and resumed.
 * The state of this object can be saved on paused, and is safely used thanks to a mutex.
 * Each component has his own coroutine scope of execution, and has potentially subscopes
 * for each input channel in order to receive requests, responses or items.
 * @property mutex the mutex that allow to safely use the state
 * @property name the name of this objet in the kt scraping line
 * @property state the state of the object
 * @property scope the coroutine scope for this component.
 * @author Sandrine Ben Mabrouk
 */
interface Component: Node {

    var state: State

    /**
     * Pauses the kt scraping component
     */
    suspend fun pause(){
    }

    /**
     * Resumes the kt scraping component
     */
    suspend fun resume(){
    }


}

abstract class AbstractComponent(): Component, AbstractProcessingNode(){
    override var state: State = State()
}

abstract class AbstractInitiatorComponent: AbstractInitiator(), Component{
    override var state: State = State()
}

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
