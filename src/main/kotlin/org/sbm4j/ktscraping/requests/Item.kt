package org.sbm4j.ktscraping.requests

import kotlinx.serialization.Serializable
import org.sbm4j.ktscraping.core.Controllable
import java.util.*

interface Item : Channelable{

    val itemId: UUID

    fun clone(): Item

}


data class ItemError(
    val ex: Exception,
    val controllable: Controllable,
    override val itemId: UUID = UUID.randomUUID()
): Item{
    override fun clone(): Item {
        return this.copy()
    }

}

data class ItemEnd(override val itemId: UUID = UUID.randomUUID()): Item{
    override fun clone(): Item {
        return this.copy()
    }
}

@Serializable
abstract class Data(){
    abstract fun clone(): Data
    fun getProperties(): Map<String, Any>{
        return mapOf()
    }
}


data class DataItem(
    val data: Data,
    override val itemId: UUID = UUID.randomUUID(),
): Item{
    override fun clone(): Item {
        return this.copy(data = data.clone() as Data)
    }
}

enum class ItemStatus{
    PROCESSED,
    ERROR,
    IGNORED
}

data class ItemAck(val itemId: UUID, val status: ItemStatus)