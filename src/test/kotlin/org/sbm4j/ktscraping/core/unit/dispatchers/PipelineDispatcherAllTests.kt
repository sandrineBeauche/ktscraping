package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherAll
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.dispatchers.Propagator
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test

class PipelineDispatcherAllTests: AbstractSendDispatcherTester() {
    override fun buildDispatcher(): Propagator {
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