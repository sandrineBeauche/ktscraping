package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.core.utils.isEndItemAckWithErrors
import org.sbm4j.ktscraping.core.utils.isOKEndItemAck
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test
import kotlin.test.assertEquals


class TestingAccumulatePipeline(name: String): AggregatePipeline(name) {

    val values = mutableListOf<Int>()

    override fun accumulateItem(item: Item) {
        val data = (item as IntDataItem).data
        values.add(data)
    }

    override fun aggregate(): List<Item> {
        val result = values.sum()
        val resultItem = IntDataItem(result, this)
        return listOf(resultItem)
    }
}

class AccumulatePipelineTests: AbstractPipelineTester<TestingAccumulatePipeline>() {

    val values = listOf(1, 5, 7, 8)

    val items = values.map{ IntDataItem(it, sender) }

    override fun buildNode(): TestingAccumulatePipeline {
        val result = TestingAccumulatePipeline("Testing accumulate")
        result.inChannel = inChannel
        result.outChannel = outChannel
        return result
    }

    suspend fun withAccumulatePipeline(inputItems: List<DataItem<*>>, nbResults: Int = 1,
                               func: AccumulatePipelineTests.(outputItems: List<Item>) -> List<ItemAck>): EventBack{
        lateinit var final: EventBack
        withConsumer() {

            //inputItems.forEach { inChannel.send(it) }

            val endItem = EndEvent(sender)
            inChannel.send(endItem)

            inputItems.forEach {
                val ack = outChannel.channel.receive()
                logger.info{ "received the ack for the item ${it}: $ack"}
            }

            val results = outChannel.getSendFlow(Item::class).take(5).toList()
            val acks = func(results)

            acks.forEach { outChannel.send(it)}

            val endEventItem = outChannel.channel.receive() as Event
            val endEventItemAck = endEventItem.buildBack()
            outChannel.send(endEventItemAck)

            logger.info { "waiting for the end event ack..." }
            final = outChannel.channel.receive() as EventBack
            logger.info { "received final ack: $final" }
        }
        return final
    }


    @Test
    fun testAccumulate1() = TestScope().runTest{
        val final = withAccumulatePipeline(items){ outputs ->
            val result = outputs[0] as IntDataItem
            logger.info { "received the data from pipeline: $result and send back ack" }
            assertEquals(values.sum(), result.data)
            val resultAck = result.buildBack()
            listOf(resultAck)
        }

        assertThat(final, isOKEndItemAck())
    }

    @Test
    fun testAccumulate2() = TestScope().runTest{
        val final = withAccumulatePipeline(items){outputs ->
            val result = outputs[0] as IntDataItem
            logger.info { "received the data from pipeline: $result and send back ack" }
            assertEquals(values.sum(), result.data)
            val error = ErrorInfo(Exception("une erreur"), node, ErrorLevel.MAJOR)
            val resultAck = result.buildErrorBack(error, Status.ERROR)
            listOf(resultAck)
        }

        assertThat(final, isEndItemAckWithErrors(Status.ERROR, 1))
    }

    @Test
    fun testAccumulate3() = TestScope().runTest{
        val final = withAccumulatePipeline(items){outputs ->
            val result = outputs[0] as IntDataItem
            logger.info { "received the data from pipeline: $result and send back ack" }
            assertEquals(values.sum(), result.data)
            listOf()
        }

        assertThat(final, isEndItemAckWithErrors(Status.ERROR, 1))
    }
}