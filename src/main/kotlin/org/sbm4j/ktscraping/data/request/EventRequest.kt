package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.StartItem
import org.sbm4j.ktscraping.data.response.EventResponse

abstract class EventRequest(
    sender: Controllable,
    override val eventName: String,
    val generateItem: Boolean = false
) : AbstractRequest(sender), Event, Cloneable {
    public override fun clone(): EventRequest {
        return super.clone() as EventRequest
    }

    override fun buildErrorBack(infos: ErrorInfo): Back<*> {
        return EventResponse(this, Status.ERROR, mutableListOf(infos))
    }

    abstract fun generateEventItem(sender: Controllable? = null): EventItem
}

data class StartRequest(
    override var sender: Controllable
): EventRequest(sender, "start", true), StartEvent{
    override fun clone(): EventRequest {
        return this.copy()
    }

    override fun generateEventItem(sender: Controllable?): EventItem {
        return if(sender != null) {
            StartItem(sender)
        } else{
            StartItem(this.sender)
        }
    }
}

data class EndRequest(
    override var sender: Controllable
): EventRequest(sender, "end", true), EndEvent{
    override fun clone(): EventRequest {
        return this.copy()
    }

    override fun generateEventItem(sender: Controllable?): EventItem {
        return if(sender != null) {
            EndItem(sender)
        } else{
            EndItem(this.sender)
        }
    }


}