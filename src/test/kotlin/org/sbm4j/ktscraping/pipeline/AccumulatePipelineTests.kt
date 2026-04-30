package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.coVerify
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.core.utils.isEndItemAckWithErrors
import org.sbm4j.ktscraping.core.utils.isEventItemAckWithErrors
import org.sbm4j.ktscraping.core.utils.isOKEndItemAck
import org.sbm4j.ktscraping.core.utils.isOKEventBackWith
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


    @Test
    fun `sum of ints`() = TestScope().runTest{
        withConsumer {
            val backs = inChannel.sendSync(items)
            val eventAgg = AggregateEvent(sender)
            val backAgg = inChannel.sendSync<EventBack>(eventAgg)

            assertThat(backAgg, isOKEventBackWith("aggregate"))
            coVerify(exactly = 1) { (stub as ComponentStub).processItem(any()) }
            val received = getReceivedItem()[0] as IntDataItem

            assertThat(received.data, equalTo(values.sum()) )
        }
    }

    @Test
    fun `sum of ints return error`() = TestScope().runTest{
        val pred = IntDataItem.predicateOnValue(21)
        stub.matches.add(pred to Exception("an error"))

        withConsumer {
            val backs = inChannel.sendSync(items)
            val eventAgg = AggregateEvent(sender)
            val backAgg = inChannel.sendSync<EventBack>(eventAgg)

            assertThat(backAgg, isEventItemAckWithErrors("aggregate", Status.ERROR, 1))
        }
    }
}