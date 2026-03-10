package org.sbm4j.ktscraping.data.item

import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import java.util.*


data class ItemAck(
    override val send: Item,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override var channelableId: UUID = UUID.randomUUID(),
    override val name: String = "${send.name}Ack"
): Back<Item> {

    override fun clone(): ItemAck {
        return this.copy()
    }
}