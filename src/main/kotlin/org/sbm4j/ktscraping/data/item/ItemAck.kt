package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import java.util.UUID


open class AbstractItemAck(
    open val itemId: UUID,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Back

data class EventItemAck(
    override val itemId: UUID,
    override val eventName: String,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
): AbstractItemAck(itemId, status, errorInfos), EventBack


data class ItemAck(
    override val itemId: UUID,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
): AbstractItemAck(itemId, status, errorInfos)