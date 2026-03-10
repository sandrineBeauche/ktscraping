package org.sbm4j.ktscraping.data.item

import org.sbm4j.meercat.channels.Send
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

abstract class Item : Send {
    companion object {
        val lastId = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    abstract override fun clone(): Item

    override fun buildErrorBack(infos: ErrorInfo, status: Status): ItemAck {
        return ItemAck(this, Status.ERROR, mutableListOf(infos))
    }

    override fun buildBack(): ItemAck {
        return ItemAck(this)
    }

}
