package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.dsl.DataItemTest
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem

class AbstractPipelineTest: AbstractPipelineTester() {
    override fun buildPipeline(pipelineName: String): AbstractPipeline {
        return object: AbstractPipeline(pipelineName){
            override suspend fun processItem(item: Item): List<Item> {
                return listOf(item)
            }
        }
    }


    @Test
    fun testPipeline() = TestScope().runTest {

        val dataVal = DataItemTest("coucou", "request1")
        val itemVal = ObjectDataItem.build(dataVal, "itemTest", sender)

        withPipeline {
            val ack = inChannel.sendSync<ItemAck>(itemVal)
            logger.debug{ "received the ack: ${ack}" }
            assertThat(ack.send.channelableId, equalTo(itemVal.channelableId))
        }
    }
}