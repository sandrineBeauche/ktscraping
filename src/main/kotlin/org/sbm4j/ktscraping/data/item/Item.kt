package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Status
import java.util.*

abstract class Item : Channelable {

    var itemId: UUID = UUID.randomUUID()

    abstract fun clone(): Item

    abstract fun generateAck(
        status: Status = Status.OK,
        errors: MutableList<ErrorInfo> = mutableListOf()
    ): AbstractItemAck
}
