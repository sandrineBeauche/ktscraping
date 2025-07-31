package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.StartItem
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.EventResponse
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


    suspend fun performEvent(eventItem: EventItem, itemAck: ItemAck){
        inChannel.send(eventItem)
        forwardInChannel.receive() as EventItem
        logger.info{"received forwarded ${eventItem.eventName} event"}

        logger.info{ "send response for ${eventItem.eventName} event"}
        outChannel.send(itemAck)
        forwardOutChannel.receive()
    }

    suspend fun performStartEvent(){
        val startItem = StartItem()
        val startItemAck = ItemAck(startItem.itemId)

        performEvent(startItem, startItemAck)
    }

    suspend fun performEndEvent(){
        val endItem = EndItem()
        val endItemAck = ItemAck(endItem.itemId)

        performEvent(endItem, endItemAck)
    }

    suspend fun withPipeline(func: suspend AbstractPipelineTester.() -> Unit){
        coroutineScope {
            pipeline.start(this)
            performStartEvent()

            func()

            performEndEvent()
            closeChannels()
            pipeline.stop()
        }
    }
}