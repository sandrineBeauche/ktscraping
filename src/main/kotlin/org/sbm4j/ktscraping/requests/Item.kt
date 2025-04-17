package org.sbm4j.ktscraping.requests

import kotlinx.serialization.Serializable
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.ProgressSlot
import org.sbm4j.ktscraping.core.ProgressState
import org.sbm4j.ktscraping.core.SlotMode
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

abstract class Item : Channelable{

    var itemId: UUID = UUID.randomUUID()

    abstract fun clone(): Item
}

enum class ErrorLevel{
    MINOR,
    MAJOR,
    FATAL
}

data class ItemError(
    val ex: Exception,
    val controllable: Controllable,
    val level: ErrorLevel,
    val data: Channelable? = null
): Item(){
    override fun clone(): Item {
        return this.copy()
    }

}


abstract class ItemProgress(
    open val slot: String,
): Item(){

    abstract fun updateProgressState(state: ProgressState)

    fun getSlot(state: ProgressState): ProgressSlot{
        return state.progress
    }
}

data class ItemStartTaskProgress(
    override val slot: String,
    val message: String = "",
    val nbSteps: Int = 0,
    val slotMode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED
): ItemProgress(slot) {

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

data class ItemStartStepProgress(
    override val slot: String,
    val message: String = "",
): ItemProgress(slot) {
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepMessage = message
    }

    override fun clone(): Item {
        return this.copy()
    }
}


data class ItemStepDoneProgress(
    override val slot: String,
    val nbSteps: Int = 1
): ItemProgress(slot){
    override fun updateProgressState(state: ProgressState) {
        val slot = getSlot(state)
        slot.stepDone += nbSteps
    }

    override fun clone(): Item {
        return this.copy()
    }
}


data class ItemEnd(val ok: Boolean = true): Item(){
    override fun clone(): Item {
        return this.copy()
    }
}

@Serializable
abstract class Data{
    abstract fun clone(): Data
    fun getProperties(): Map<String, Any>{
        return mapOf()
    }
}


data class DataItem<T: Data>(
    val data: T,
    val clazz: KClass<T>,
    val label: String = "data"
): Item(){
    companion object{
        inline fun <reified T: Data> build(data: T, label: String): DataItem<T> {
            return DataItem(data, T::class, label)
        }
    }


    override fun clone(): Item {
        val result = this.copy(data = clazz.cast(data.clone()))
        result.itemId = this.itemId
        return result
    }
}


abstract class StandardFormatItem<T>(
    open val data: T
): Item(){
    abstract fun prettyPrint(): String
}

enum class ItemStatus{
    PROCESSED,
    ERROR,
    IGNORED
}

data class ItemAck(val itemId: UUID, val status: ItemStatus, val error: Exception? = null)