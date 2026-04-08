package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherAll
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.dispatchers.Propagator
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test

class PipelineDispatcherAllTests: AbstractSendDispatcherTester<PipelineDispatcherAll>() {

    override val nbChannelsOuts: Int = 3

    override fun buildNode(): PipelineDispatcherAll {
        val result = PipelineDispatcherAll("PipelineDispatcher", di)
        result.channelIn = channelIn
        result.channelOuts.addAll(channelOuts)
        return result
    }

    @Test
    fun `send item`() = TestScope().runTest {
        val item = IntDataItem(1, sender)

        val ack= channelIn.sendSync<ItemAck>(item)
        logger.debug{"Received response: $ack"}

        verifyNbInvocations(listOf(1, 1, 1))
    }

    @Test
    fun `send event on pipeline`() = testScope.runTest {
        val event = StartEvent(sender)

        val response = channelIn.sendSync<EventBack>(event)
        logger.debug{"Received response: $response"}

        verifyNbInvocations(listOf(1, 1, 1))
    }
}