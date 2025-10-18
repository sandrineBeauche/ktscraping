package org.sbm4j.ktscraping.data.internal

import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.ProgressSlot
import org.sbm4j.ktscraping.core.ProgressState
import org.sbm4j.ktscraping.core.SlotMode
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status

abstract class ProgressInternal(
    open val slot: String,
): Internal(){


    abstract fun updateProgressState(state: ProgressState)

    fun getSlot(state: ProgressState): ProgressSlot{
        return state.progress
    }


}

data class StartTaskProgressInternal(
    override val slot: String,
    val message: String = "",
    val nbSteps: Int = 0,
    val slotMode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED,
    override var sender: Controllable,
    override val name: String = "startTask-${slot}"
): ProgressInternal(slot) {

    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.taskMessage = message
        slot.totalSteps = nbSteps
        slot.stepDone = 0
        slot.mode = slotMode
    }

    override fun clone(): Internal {
        return this.copy()
    }

    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        TODO("Not yet implemented")
    }

    override fun buildBack(): Back<*> {
        TODO("Not yet implemented")
    }
}

data class StartStepProgressItem(
    override val slot: String,
    val message: String = "",
    override var sender: Controllable,
    override val name: String = "StartStep-${slot}",
): ProgressInternal(slot) {
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepMessage = message
    }

    override fun clone(): Internal {
        return this.copy()
    }

    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        TODO("Not yet implemented")
    }

    override fun buildBack(): Back<*> {
        TODO("Not yet implemented")
    }
}


data class StepDoneProgressItem(
    override val slot: String,
    val nbSteps: Int = 1,
    override var sender: Controllable,
    override val name: String = "StepDone-${slot}"
): ProgressInternal(slot){
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepDone += nbSteps
    }

    override fun clone(): Internal {
        return this.copy()
    }

    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        TODO("Not yet implemented")
    }

    override fun buildBack(): Back<*> {
        TODO("Not yet implemented")
    }
}