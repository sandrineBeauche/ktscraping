package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.DataItemAck
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.StartItem
import kotlin.test.BeforeTest

abstract class AbstractPipelineTester: DualScrapingTest<Item, AbstractItemAck>() {

    lateinit var pipeline: AbstractPipeline

    val pipelineName: String = "Pipeline"

    abstract fun buildPipeline(pipelineName: String): AbstractPipeline

    @BeforeTest
    open fun setUp(){
        initChannels()
        clearAllMocks()

        pipeline = spyk(buildPipeline(pipelineName))

        every { pipeline.itemIn } returns inChannel
        every { pipeline.itemOut } returns forwardInChannel
        every { pipeline.itemAckIn } returns outChannel
        every { pipeline.itemAckOut } returns forwardOutChannel
    }


    suspend fun performEvent(eventItem: EventItem, dataItemAck: EventItemAck){
        inChannel.send(eventItem)
        forwardInChannel.receive() as EventItem
        logger.info{"received forwarded ${eventItem.eventName} event"}

        logger.info{ "send response for ${eventItem.eventName} event"}
        outChannel.send(dataItemAck)
        forwardOutChannel.receive()
    }

    suspend fun performStartEvent(){
        val startItem = StartItem()
        val startItemAck = EventItemAck(startItem.itemId, startItem.eventName)

        performEvent(startItem, startItemAck)
    }

    suspend fun performEndEvent(){
        val endItem = EndItem()
        val endItemAck = EventItemAck(endItem.itemId, endItem.eventName)

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