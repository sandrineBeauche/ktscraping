package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.requests.ItemProgress


enum class SlotMode{
    TEXT_ONLY,
    PROGRESS_BAR_UNDEFINED,
    PROGRESS_BAR_DEFINED
}

data class ProgressSlot(
    val name: String,
    var taskMessage: String = "",
    var stepMessage: String = "",
    var stepDone: Int = 0,
    var totalSteps: Int = 100,
    var mode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED
)

data class ProgressState(
    val slotName: String,
    val progress: ProgressSlot
)


class ProgressMonitor() {

    var receivedRequest: Int = 0
    var receivedResponse: Int = 0
    var receivedItem: Int = 0
    var receivedItemAck: Int = 0


    val progressChannel: Channel<ProgressState> = Channel(Channel.UNLIMITED)

    val progressState: MutableMap<String, ProgressState> = mutableMapOf()

    suspend fun processItemProgress(item: ItemProgress){
        val state = progressState.getOrPut(item.slot){ ProgressState(item.slot, ProgressSlot(item.slot)) }
        item.updateProgressState(state)
        progressChannel.send(state.copy())
    }

}