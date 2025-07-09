package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent

abstract class EventRequest(
    sender: RequestSender,
    override val eventName: String,
    val generateItem: Boolean = false
) : AbstractRequest(sender), Event {
}

data class StartRequest(
    override val sender: RequestSender
): EventRequest(sender, "start", true), StartEvent

data class EndRequest(
    override val sender: RequestSender
): EventRequest(sender, "end", true), EndEvent