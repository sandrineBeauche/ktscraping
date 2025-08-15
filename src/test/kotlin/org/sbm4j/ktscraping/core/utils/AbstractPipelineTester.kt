package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.StartItem
import kotlin.test.BeforeTest

abstract class AbstractPipelineTester: DualScrapingTest<Item, AbstractItemAck<*>>() {

    lateinit var pipeline: AbstractPipeline

    val pipelineName: String = "Pipeline"

    val sender: Controllable = mockk<Controllable>()

    abstract fun buildPipeline(pipelineName: String): AbstractPipeline

    @BeforeTest
    open fun setUp(){
        initChannels()
        clearAllMocks()

        pipeline = spyk(buildPipeline(pipelineName))

        every { pipeline.inChannel } returns inChannel
        every { pipeline.outChannel } returns outChannel
    }


    suspend fun performEvent(eventItem: EventItem, dataItemAck: EventItemAck){
        inChannel.send(eventItem)
        outChannel.channel.receive() as EventItem
        logger.info{"received forwarded ${eventItem.eventName} event"}

        logger.info{ "send response for ${eventItem.eventName} event"}
        outChannel.send(dataItemAck)
        inChannel.channel.receive()
    }

    suspend fun performStartEvent(){
        val startItem = StartItem(sender)
        val startItemAck = EventItemAck(startItem)

        performEvent(startItem, startItemAck)
    }

    suspend fun performEndEvent(){
        val endItem = EndItem(sender)
        val endItemAck = EventItemAck(endItem)

        performEvent(endItem, endItemAck)
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