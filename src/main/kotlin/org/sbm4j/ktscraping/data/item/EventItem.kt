package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.Status

abstract class EventItem(
    override val eventName: String
): Item(), Event{
    override fun generateAck(status: Status, errors: MutableList<ErrorInfo>): EventItemAck {
        return EventItemAck(this.itemId, this.eventName, status, errors)
    }
}

data class StartItem(val ok: Boolean = true): EventItem("start"), StartEvent {
    override fun clone(): Item {
        val result =  this.copy()
        result.itemId = this.itemId
        return result
    }
}

data class EndItem(val ok: Boolean = true): EventItem("end"), EndEvent{
    override fun clone(): Item {
        val result = this.copy()
        result.itemId = this.itemId
        return result
    }
}