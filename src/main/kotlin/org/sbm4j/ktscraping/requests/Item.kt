package org.sbm4j.ktscraping.requests

import java.util.*

interface Item : Channelable{

    val id: UUID

    fun clone(): Item

}

abstract class AbstractItem(override val id: UUID = UUID.randomUUID()): Item{

}

enum class ItemStatus{
    PROCESSED,
    ERROR,
    IGNORED
}

data class ItemAck(val itemId: UUID, val status: ItemStatus)