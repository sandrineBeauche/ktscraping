package org.sbm4j.ktscraping.data.events

import org.sbm4j.meercat.channels.Send
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.meercat.components.SendSource
import java.util.UUID

enum class EventPropagation{
    DOWNLOADER,
    PIPELINE,
    BOTH,
    NONE
}

abstract class Event(
    override var sender: SendSource,
    val eventName: String,
    val propagation: EventPropagation = EventPropagation.BOTH,
    override val name: String = "${eventName}-Event"
): Send {

    override var channelableId: UUID = UUID.randomUUID()

    override fun buildBack(): EventBack {
        return EventBack(this)
    }

    override fun buildErrorBack(infos: ErrorInfo, status: Status): EventBack {
        return EventBack(this, Status.ERROR, mutableListOf(infos))
    }

    public abstract override fun clone(): Event

}

data class EventBack(
    override val send: Event,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "${send.eventName}-EventBack"
): Back<Event>{
    override var channelableId: UUID = UUID.randomUUID()

    override fun clone(): Back<Event> {
        return this.copy()
    }
}

data class StartEvent(
    override var sender: SendSource
): Event(sender, "start"){

    override fun clone(): Event {
        return this.copy()
    }
}

data class EndEvent(
    override var sender: SendSource
): Event(sender, "end"){

    override fun clone(): Event {
        return this.copy()
    }
}