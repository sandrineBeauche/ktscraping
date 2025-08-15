package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status
import java.util.*


abstract class AbstractItemAck<T: Item>(
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Back<T> {
    override var channelableId: UUID = UUID.randomUUID()
}

data class EventItemAck(
    override val send: EventItem,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): AbstractItemAck<EventItem>(status, errorInfos){


}


data class DataItemAck(
    override val send: DataItem<*>,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
): AbstractItemAck<DataItem<*>>(status, errorInfos){

}