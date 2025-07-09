package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent

abstract class EventItem(
    override val eventName: String
): Item(), Event{

}

data class StartItem(val ok: Boolean = true): EventItem("start"), StartEvent {
    override fun clone(): Item {
        return this.copy()
    }
}

data class EndItem(val ok: Boolean = true): EventItem("end"), EndEvent{
    override fun clone(): Item {
        return this.copy()
    }
}