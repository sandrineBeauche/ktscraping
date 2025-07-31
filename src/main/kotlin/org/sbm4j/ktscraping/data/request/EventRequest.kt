package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.StartItem

abstract class EventRequest(
    sender: RequestSender,
    override val eventName: String,
    val generateItem: Boolean = false
) : AbstractRequest(sender), Event, Cloneable {
    public override fun clone(): EventRequest {
        return super.clone() as EventRequest
    }

    abstract fun generateEventItem(): EventItem
}

data class StartRequest(
    override val sender: RequestSender
): EventRequest(sender, "start", true), StartEvent{
    override fun clone(): EventRequest {
        return this.copy()
    }

    override fun generateEventItem(): EventItem {
        return StartItem()
    }
}

data class EndRequest(
    override val sender: RequestSender
): EventRequest(sender, "end", true), EndEvent{
    override fun clone(): EventRequest {
        return this.copy()
    }

    override fun generateEventItem(): EventItem {
        return EndItem()
    }


}