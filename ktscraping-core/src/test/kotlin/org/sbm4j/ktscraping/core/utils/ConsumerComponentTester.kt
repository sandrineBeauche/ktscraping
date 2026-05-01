package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

interface ConsumerComponentTester {
    var inChannel: SuperChannel

    val sender: SendSource

    open suspend fun doStartEvent(){
        logger.debug{"Do Start event"}
        val startEvent = StartEvent(sender)
        inChannel.sendSync<EventBack>(startEvent)
        logger.debug{"Start event done"}
    }

    open suspend fun doEndEvent(){
        logger.debug{"Do End event"}
        val endEvent = EndEvent(sender)
        inChannel.sendSync<EventBack>(endEvent)
        logger.debug{"End event done"}
    }

    suspend fun withConsumer(func: suspend ConsumerComponentTester.() -> Unit) {
        doStartEvent()
        func()
        doEndEvent()
    }
}