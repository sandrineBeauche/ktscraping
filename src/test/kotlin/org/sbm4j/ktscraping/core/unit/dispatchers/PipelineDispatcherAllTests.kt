package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherAll
import org.sbm4j.ktscraping.core.dispatchers.SendPropagator
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.data.item.ItemAck
import kotlin.test.Test

class PipelineDispatcherAllTests: AbstractSendDispatcherTester() {
    override fun buildDispatcher(): SendPropagator {
        return PipelineDispatcherAll("PipelineDispatcher", di)
    }

    @Test
    fun testSendItem() = TestScope().runTest {
        val item = IntDataItem(1, sender)

        withDispatcher(listOf(1, 1, 1)){
            val ack= inChannel.sendSync<ItemAck>(item)
            logger.debug{"Received response: $ack"}
        }
    }
}