package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherOne
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.dispatchers.Propagator
import kotlin.test.Test

class TestingPipelineDispatcherOne(override val di: DI): PipelineDispatcherOne("TestingPipelineDispatcherOne", di = di){
    override fun selectChannel(item: Item): SuperChannel {
        val key = (item as DataItem<*>).data as Int
        return this.channelOuts[key]
    }

}

class PipelineDispatcherOneTests:
    AbstractSendDispatcherTester<TestingPipelineDispatcherOne>()
{
    override val nbChannelsOuts: Int = 3

    override fun buildNode(): TestingPipelineDispatcherOne {
        val result = TestingPipelineDispatcherOne(di)
        result.channelIn = channelIn
        result.channelOuts.addAll(channelOuts)
        return result
    }

    @Test
    fun testSendItem() = TestScope().runTest {
        val item = IntDataItem(1, sender)

        val ack= channelIn.sendSync<ItemAck>(item)
        logger.debug{"Received response: $ack"}

        verifyNbInvocations(listOf(0, 1, 0))
    }

    @Test
    fun `send event on pipeline`() = testScope.runTest {
        val event = StartEvent(sender)

        val response = channelIn.sendSync<EventBack>(event)
        logger.debug{"Received response: $response"}

        verifyNbInvocations(listOf(1, 1, 1))
    }
}