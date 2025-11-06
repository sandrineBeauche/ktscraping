package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.BeforeTest

abstract class AbstractPipelineTester: DualScrapingTest() {

    lateinit var pipeline: AbstractPipeline

    val pipelineName: String = "Pipeline"

    abstract fun buildPipeline(pipelineName: String): AbstractPipeline

    @BeforeTest
    open fun setUp(){
        initChannels()
        clearAllMocks()

        pipeline = spyk(buildPipeline(pipelineName))

        every { pipeline.inChannel } returns inChannel
        every { pipeline.outChannel } returns outChannel
    }


    suspend fun performEvent(eventItem: Event, dataItemAck: EventBack){
        inChannel.send(eventItem)
        outChannel.channel.receive() as Event
        logger.info{"received forwarded ${eventItem.eventName} event"}

        logger.info{ "send response for ${eventItem.eventName} event"}
        outChannel.send(dataItemAck)
        inChannel.channel.receive()
    }

    suspend fun performStartEvent(){
        val startEvent = StartEvent(sender)
        val startEventBack = startEvent.buildBack()

        performEvent(startEvent, startEventBack)
    }

    suspend fun performEndEvent(){
        val endEvent = EndEvent(sender)
        val endItemAck = endEvent.buildBack()

        performEvent(endEvent, endItemAck)
    }

    suspend fun withPipeline(
        startEvent: Boolean = true,
        endEvent: Boolean = true,
        func: suspend AbstractPipelineTester.() -> Unit)
    {
        coroutineScope {
            pipeline.start(this)

            if(startEvent) {
                performStartEvent()
            }

            func()

            if(endEvent) {
                performEndEvent()
            }

            closeChannels()
            pipeline.stop()
        }
    }
}