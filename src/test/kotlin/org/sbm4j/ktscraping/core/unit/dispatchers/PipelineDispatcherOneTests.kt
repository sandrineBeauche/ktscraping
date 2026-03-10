package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherOne
import org.sbm4j.ktscraping.core.dispatchers.SendPropagator
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import kotlin.test.Test

class TestingPipelineDispatcherOne(override val di: DI): PipelineDispatcherOne("TestingPipelineDispatcherOne", di = di){
    override fun selectChannel(item: Item): SuperChannel {
        val key = (item as DataItem<*>).data as Int
        return this.receivers[key]
    }

}

class PipelineDispatcherOneTests: AbstractSendDispatcherTester() {
    override fun buildDispatcher(): SendPropagator {
        return TestingPipelineDispatcherOne(di)
    }

    @Test
    fun testSendItem() = TestScope().runTest {
        val item = IntDataItem(1, sender)

        withDispatcher(listOf(0, 1, 0)){
            val ack= inChannel.sendSync<ItemAck>(item)
            logger.debug{"Received response: $ack"}
        }
    }

}