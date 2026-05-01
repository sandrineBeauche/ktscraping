package org.sbm4j.ktscraping.data.internal

import org.sbm4j.ktscraping.core.ProgressSlot
import org.sbm4j.ktscraping.core.ProgressState
import org.sbm4j.ktscraping.core.SlotMode
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import org.sbm4j.meercat.data.Status

/**
 * Base class for internal messages reporting scraping progress to the Engine.
 *
 * Progress is organized into named [slot]s, each representing an independent task
 * running concurrently (e.g. downloading, parsing, exporting). Each [ProgressInternal]
 * targets a specific slot and updates its state via [updateProgressState].
 *
 * The barrier key is the [slot] name, allowing [Barrier] nodes to synchronize
 * branches by task.
 *
 * @property slot Name of the progress slot (task) this message targets.
 *
 * @see ProgressState
 * @see ProgressSlot
 */
abstract class ProgressInternal(
    open val slot: String,
): Internal(){

    /**
     * Updates the [ProgressSlot] identified by [slot] inside the given [ProgressState].
     * Each subclass applies its own update logic (start task, start step, step done, etc.).
     *
     * @param state The global progress state holding all concurrent task slots.
     */
    abstract fun updateProgressState(state: ProgressState)

    /**
     * Retrieves the [ProgressSlot] associated with [slot] from the given [ProgressState].
     *
     * @param state The global progress state.
     * @return The [ProgressSlot] corresponding to this message's [slot].
     */
    fun getSlot(state: ProgressState): ProgressSlot{
        return state.progress
    }

    /**
     * Returns [slot] as the barrier key, allowing [Barrier] nodes to synchronize
     * branches by task name.
     */
    override fun getKeyBarrier(): String {
        return this.slot
    }

}

/**
 * Internal message signaling the start of a new task in a progress slot.
 *
 * Initializes the target [slot] with a task message, total number of steps,
 * and display mode. Resets [ProgressSlot.stepDone] to zero.
 *
 * @property slot Name of the progress slot to initialize.
 * @property message Human-readable description of the task being started.
 * @property nbSteps Total number of steps expected for this task. Defaults to `0` if unknown.
 * @property slotMode Display mode for this slot, defaults to [SlotMode.PROGRESS_BAR_DEFINED].
 * @property sender The Spider emitting this notification.
 * @property name Technical name of the message, defaults to `"startTask-<slot>"`.
 *
 * @see SlotMode
 */
data class StartTaskProgressInternal(
    override val slot: String,
    val message: String = "",
    val nbSteps: Int = 0,
    val slotMode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED,
    override var sender: SendSource,
    override val name: String = "startTask-${slot}"
): ProgressInternal(slot) {

    /**
     * Initializes the target slot: sets the task message, total steps, resets done steps
     * to zero and applies the display mode.
     */
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.taskMessage = message
        slot.totalSteps = nbSteps
        slot.stepDone = 0
        slot.mode = slotMode
    }

    /** Creates a copy of this message via [copy]. */
    override fun clone(): Internal {
        return this.copy()
    }

}

/**
 * Internal message signaling the start of a new step within an ongoing task.
 *
 * Updates the step message displayed in the target [slot] while the task is running.
 *
 * @property slot Name of the progress slot to update.
 * @property message Human-readable description of the step being started.
 * @property sender The Spider emitting this notification.
 * @property name Technical name of the message, defaults to `"StartStep-<slot>"`.
 */
data class StartStepProgressItem(
    override val slot: String,
    val message: String = "",
    override var sender: SendSource,
    override val name: String = "StartStep-${slot}",
): ProgressInternal(slot) {

    /**
     * Updates the step message of the target slot.
     */
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepMessage = message
    }

    /** Creates a copy of this message via [copy]. */
    override fun clone(): Internal {
        return this.copy()
    }
}

/**
 * Internal message reporting that one or more steps have been completed in a task.
 *
 * Increments [ProgressSlot.stepDone] by [nbSteps] in the target slot, allowing
 * the UI to advance the progress bar accordingly.
 *
 * @property slot Name of the progress slot to update.
 * @property nbSteps Number of steps completed, defaults to `1`.
 * @property sender The Spider emitting this notification.
 * @property name Technical name of the message, defaults to `"StepDone-<slot>"`.
 */
data class StepDoneProgressItem(
    override val slot: String,
    val nbSteps: Int = 1,
    override var sender: SendSource,
    override val name: String = "StepDone-${slot}"
): ProgressInternal(slot){

    /**
     * Increments the number of completed steps in the target slot by [nbSteps].
     */
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepDone += nbSteps
    }

    /** Creates a copy of this message via [copy]. */
    override fun clone(): Internal {
        return this.copy()
    }
}