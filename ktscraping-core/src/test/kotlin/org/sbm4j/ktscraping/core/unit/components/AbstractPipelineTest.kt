package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.logger

class TestingPipeline: AbstractPipeline("pipeline"){
    override suspend fun processItem(item: Item): List<Item> {
        if(item is IntDataItem){
            item.data++
        }
        return listOf(item)
    }
}

class AbstractPipelineTest: AbstractPipelineTester<TestingPipeline>() {

    override fun buildNode(): TestingPipeline {
        val result = TestingPipeline()
        result.inChannel = inChannel
        result.outChannel = outChannel
        return result
    }


    @Test
    fun `forward item and ack`() = testScope.runTest {

        val item = IntDataItem(1, sender)
        lateinit var ack: ItemAck

        withConsumer {
            ack = inChannel.sendSync<ItemAck>(item)
            logger.debug{ "received the ack: ${ack}" }
        }

        assertThat(ack.send.channelableId, equalTo(item.channelableId))
        val captured = getReceivedSend()
        assertThat(captured[1], isA<IntDataItem>(
            has(IntDataItem::data, equalTo(2)),
        ))
    }
}