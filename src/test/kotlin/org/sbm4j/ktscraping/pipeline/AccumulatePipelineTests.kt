package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.core.utils.isEndItemAckWithErrors
import org.sbm4j.ktscraping.core.utils.isOKEndItemAck
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.DataItemAck
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.Item
import kotlin.test.Test
import kotlin.test.assertEquals

data class IntDataItem(override val data: Int): DataItem<Int>(){
    override fun clone(): Item {
        return this.copy()
    }
}

class TestingAccumulatePipeline(name: String): AccumulatePipeline(name) {

    val values = mutableListOf<Int>()

    override fun accumulateItem(item: DataItem<*>) {
        val data = item.data as Int
        values.add(data)
    }

    override fun generateItems(): List<Item> {
        val result = values.sum()
        val resultItem = IntDataItem(result)
        return listOf(resultItem)
    }
}

class AccumulatePipelineTests: AbstractPipelineTester() {

    val values = listOf(1, 5, 7, 8)

    val items = values.map{ IntDataItem(it) }

    override fun buildPipeline(pipelineName: String): AbstractPipeline {
        return TestingAccumulatePipeline("Testing accumulate")
    }


    suspend fun withAccumulatePipeline(inputItems: List<DataItem<*>>, nbResults: Int = 1,
                               func: AccumulatePipelineTests.(outputItems: List<DataItem<*>>) -> List<DataItemAck>): EventItemAck{
        lateinit var final: EventItemAck
        withPipeline(endEvent = false) {
            inputItems.forEach { inChannel.send(it) }

            val endItem = EndItem()
            inChannel.send(endItem)

            inputItems.forEach {
                val ack = forwardOutChannel.receive()
                logger.info{ "received the ack for the item ${it}: $ack"}
            }

            val results = forwardInChannel.receiveAsFlow().take(nbResults).toList() as List<DataItem<*>>
            val acks = func(results)

            acks.forEach { outChannel.send(it)}

            val endEventItem = forwardInChannel.receive() as EventItem
            val endEventItemAck = endEventItem.generateAck()
            outChannel.send(endEventItemAck)

            logger.info { "waiting for the end event ack..." }
            final = forwardOutChannel.receive() as EventItemAck
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
            val resultAck = DataItemAck(result.itemId)
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
            val error = ErrorInfo(Exception("une erreur"), this.pipeline, ErrorLevel.MAJOR)
            val resultAck = DataItemAck(result.itemId, Status.ERROR, mutableListOf(error))
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