package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.data.internal.ProgressInternal

/**
 * Defines the display mode for a [ProgressSlot] in the progress UI.
 */
enum class SlotMode{
    /** Display a text message only, no progress bar. */
    TEXT_ONLY,

    /**
     * Display a progress bar with an indeterminate total.
     * Use when the total number of steps is not known in advance.
     */

    PROGRESS_BAR_UNDEFINED,

    /**
     * Display a progress bar with a defined total.
     * Use when the total number of steps is known upfront via [ProgressSlot.totalSteps].
     */
    PROGRESS_BAR_DEFINED
}

/**
 * Represents the progress state of a single concurrent scraping task.
 *
 * Each [ProgressSlot] tracks the progress of one named task running in the Spider,
 * including the current task and step messages, the number of completed steps,
 * the total expected steps, and the display mode.
 *
 * Slots are identified by [name] and updated via [ProgressInternal] messages.
 *
 * @property name The unique identifier of this slot, matching [ProgressInternal.slot].
 * @property taskMessage Human-readable description of the current task.
 * @property stepMessage Human-readable description of the current step.
 * @property stepDone Number of steps completed so far.
 * @property totalSteps Total number of steps expected, defaults to `100`.
 * @property mode Display mode for this slot, defaults to [SlotMode.PROGRESS_BAR_DEFINED].
 *
 * @see ProgressInternal
 * @see SlotMode
 */
data class ProgressSlot(
    val name: String,
    var taskMessage: String = "",
    var stepMessage: String = "",
    var stepDone: Int = 0,
    var totalSteps: Int = 100,
    var mode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED
)

/**
 * Snapshot of the progress state for a named slot, emitted to the [ProgressMonitor.progressChannel].
 *
 * Each time a [ProgressInternal] message is processed, an updated [ProgressState] snapshot
 * is sent to [ProgressMonitor.progressChannel] for consumption by the UI.
 *
 * @property slotName The name of the slot this state belongs to.
 * @property progress The current [ProgressSlot] state snapshot.
 *
 * @see ProgressSlot
 * @see ProgressMonitor
 */
data class ProgressState(
    val slotName: String,
    val progress: ProgressSlot
)

/**
 * Monitors scraping progress and exposes it to the UI via [progressChannel].
 *
 * [ProgressMonitor] is a shared object between the [Engine] and the UI:
 * - The [Engine] increments the request/response/item counters as messages transit.
 * - [processItemProgress] updates the [progressState] map and emits a [ProgressState]
 *   snapshot to [progressChannel] for real-time UI consumption (e.g. progress bars).
 *
 * Progress is organized into named slots ([ProgressSlot]), one per concurrent scraping
 * task. Each slot is created on first use and updated by [ProgressInternal] messages
 * emitted by the Spider.
 *
 * Note: integration between the [Engine] counters and [progressChannel] is not yet
 * fully implemented.
 *
 * @see ProgressSlot
 * @see ProgressState
 * @see ProgressInternal
 * @see Engine
 */
class ProgressMonitor() {


    var receivedItemAck: Int = 0

    /**
     * Unbounded channel emitting [ProgressState] snapshots for UI consumption.
     *
     * Each time a [ProgressInternal] is processed, an updated snapshot of the
     * affected slot is sent to this channel. UI components can collect from this
     * channel to update progress bars or status messages in real time.
     */
    val progressChannel: Channel<ProgressState> = Channel(Channel.UNLIMITED)

    /**
     * Current progress state for all active slots, keyed by slot name.
     *
     * Slots are created on first use when a [ProgressInternal] targeting an unknown
     * slot name is received.
     */
    val progressState: MutableMap<String, ProgressState> = mutableMapOf()

    /**
     * Processes a [ProgressInternal] message by updating the corresponding slot
     * and emitting a fresh [ProgressState] snapshot to [progressChannel].
     *
     * If no slot exists for [ProgressInternal.slot], a new one is created with default values.
     *
     * @param item The progress notification emitted by the Spider.
     */
    suspend fun processItemProgress(item: ProgressInternal){
        val state = progressState.getOrPut(item.slot){ ProgressState(item.slot, ProgressSlot(item.slot)) }
        item.updateProgressState(state)
        progressChannel.send(state.copy())
    }

}