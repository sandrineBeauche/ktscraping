
package org.sbm4j.ktscraping.data.events

import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.UUID

/**
 * Defines which branches of the KtScraping topology an [Event] should be propagated to.
 *
 * The [Engine] contains three sub-components subscribed to the same input channel, each
 * filtering events according to their [EventPropagation] :
 * - [DOWNLOADER] : handled only by the Downloader sub-component
 * - [PIPELINE] : handled only by the Pipeline sub-component
 * - [BOTH] : handled by the BothBroadcaster, which emits to both branches and aggregates the backs
 * - [NONE] : silently ignored by all sub-components
 */
enum class EventPropagation{
    /** The event is propagated only to the Downloader branch. */
    DOWNLOADER,
    /** The event is propagated only to the Pipeline branch. */
    PIPELINE,
    /**
     * The event is propagated to both branches simultaneously.
     * The BothBroadcaster waits for backs from both branches before returning
     * an aggregated back to the Spider.
     */
    BOTH,

    /** The event is not propagated to any branch and is silently ignored. */
    NONE
}


/**
 * Base message representing an event in the KtScraping topology.
 *
 * Events are emitted by Spiders ([Initiator]) and routed by the [Engine]
 * to the Downloader and/or Pipeline branches according to their [propagation].
 * Each event carries an [eventName] used as a barrier key for
 * synchronization in the [Barrier] nodes of the topology.
 *
 * @param sender The source emitting this event, typically a Spider.
 * @param eventName Functional name of the event (e.g. `"start"`, `"end"`).
 * @param propagation Target branches for this event, defaults to [EventPropagation.BOTH].
 * @param name Technical name of the message, defaults to `"<eventName>-Event"`.
 *
 * @see EventBack
 * @see EventPropagation
 * @see Engine
 */
abstract class Event(
    override var sender: SendSource,
    open val eventName: String,
    open val propagation: EventPropagation = EventPropagation.BOTH,
    override val name: String = "${eventName}-Event"
): Send {

    override var channelableId: UUID = UUID.randomUUID()

    /**
     * Builds the nominal back message associated with this event.
     * @return An [EventBack] with a [Status.OK] status.
     */
    override fun buildBack(): EventBack {
        return EventBack(this)
    }

    /**
     * Builds an error back message associated with this event.
     * @param infos Information about the error that occurred.
     * @param status Error status to apply.
     * @return An [EventBack] with the provided status and error info.
     */
    override fun buildErrorBack(infos: ErrorInfo, status: Status): EventBack {
        return EventBack(this, Status.ERROR, mutableListOf(infos))
    }

    /** Creates a copy of this event. Must be implemented by each subclass. */
    abstract override fun clone(): Event

    /**
     * Returns [eventName] as the barrier key, allowing [Barrier] nodes in the topology
     * to synchronize branches on the event name.
     */
    override fun getKeyBarrier(): String {
        return this.eventName
    }
}

/**
 * Back message associated with an [Event], returned to the emitting Spider.
 *
 * Aggregates the processing status and any errors accumulated
 * while passing through the various branches of the topology.
 * In the [EventPropagation.BOTH] case, this back is produced by the BothBroadcaster
 * after receiving and aggregating the backs from both branches.
 *
 * @property send The original [Event] this back is responding to.
 * @property status Overall processing status, defaults to [Status.OK].
 * @property errorInfos List of errors accumulated during processing.
 * @property name Technical name of the message, defaults to `"<eventName>-EventBack"`.
 */
data class EventBack(
    override val send: Event,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "${send.eventName}-EventBack"
): Back<Event>{
    override var channelableId: UUID = UUID.randomUUID()

    /** Creates a copy of this back via [copy]. */
    override fun clone(): Back<Event> {
        return this.copy()
    }
}
