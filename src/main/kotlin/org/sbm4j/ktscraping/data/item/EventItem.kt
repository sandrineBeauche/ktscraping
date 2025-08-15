package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.Status

abstract class EventItem(
    override val eventName: String,
    override var sender: Controllable
): Item(), Event{
    override fun generateAck(status: Status, errors: MutableList<ErrorInfo>): AbstractItemAck<*> {
        return EventItemAck(this, status, errors)
    }

    override val name: String = "EventItem"

    override fun buildErrorBack(infos: ErrorInfo): Back<*> {
        return EventItemAck(this, Status.ERROR, mutableListOf(infos))
    }
}

data class StartItem(override var sender: Controllable): EventItem("start", sender), StartEvent {
    override fun clone(): Item {
        val result =  this.copy()
        result.channelableId = this.channelableId
        return result
    }
}

data class EndItem(override var sender: Controllable): EventItem("end", sender), EndEvent{
    override fun clone(): Item {
        val result = this.copy()
        result.channelableId = this.channelableId
        return result
    }
}