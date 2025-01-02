package org.sbm4j.ktscraping.requests

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

interface Item : Channelable{

    val id: UUID

    fun clone(): Item

}

data class ItemEnd(override val id: UUID = UUID.randomUUID()): Item{
    override fun clone(): Item {
        return this.copy()
    }
}

@Serializable
abstract class AbstractItem(
    @Contextual override val id: UUID = UUID.randomUUID()
): Item{

    fun getProperties(): Map<String, Any>{
        return mapOf()
    }
}

enum class ItemStatus{
    PROCESSED,
    ERROR,
    IGNORED
}

data class ItemAck(val itemId: UUID, val status: ItemStatus)