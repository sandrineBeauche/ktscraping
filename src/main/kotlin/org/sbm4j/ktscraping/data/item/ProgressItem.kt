package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.core.ProgressSlot
import org.sbm4j.ktscraping.core.ProgressState
import org.sbm4j.ktscraping.core.SlotMode

abstract class ProgressItem(
    open val slot: String,
): Item(){

    abstract fun updateProgressState(state: ProgressState)

    fun getSlot(state: ProgressState): ProgressSlot{
        return state.progress
    }
}

data class StartTaskProgressItem(
    override val slot: String,
    val message: String = "",
    val nbSteps: Int = 0,
    val slotMode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED
): ProgressItem(slot) {

    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.taskMessage = message
        slot.totalSteps = nbSteps
        slot.stepDone = 0
        slot.mode = slotMode
    }

    override fun clone(): Item {
        return this.copy()
    }
}

data class StartStepProgressItem(
    override val slot: String,
    val message: String = "",
): ProgressItem(slot) {
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepMessage = message
    }

    override fun clone(): Item {
        return this.copy()
    }
}


data class StepDoneProgressItem(
    override val slot: String,
    val nbSteps: Int = 1
): ProgressItem(slot){
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepDone += nbSteps
    }

    override fun clone(): Item {
        return this.copy()
    }
}