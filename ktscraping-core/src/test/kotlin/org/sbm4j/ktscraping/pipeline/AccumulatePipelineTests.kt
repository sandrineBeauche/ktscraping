package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.utils.isEventItemAckWithErrors
import org.sbm4j.ktscraping.utils.isOKEventBackWith
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.data.Status
import kotlin.test.Test


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