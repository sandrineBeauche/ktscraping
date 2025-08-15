package org.sbm4j.ktscraping.core.unit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.dsl.DataItemTest
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.DataItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem

class AbstractPipelineTest: AbstractPipelineTester() {
    override fun buildPipeline(pipelineName: String): AbstractPipeline {
        return object: AbstractPipeline(pipelineName){
            override suspend fun processDataItem(item: DataItem<*>): List<Item> {
                return listOf(item)
            }
        }
    }


    @Test
    fun testPipeline() = TestScope().runTest {

        val dataVal = DataItemTest("coucou", "request1")
        val itemVal = ObjectDataItem.build(dataVal, "itemTest", sender)

        withPipeline {
            inChannel.send(itemVal)
            val processed = outChannel.channel.receive() as ObjectDataItem<*>

            val ack = processed.generateAck()
            outChannel.send(ack)
            val receivedAck = inChannel.channel.receive()

            assertThat(receivedAck.channelableId, equalTo(itemVal.channelableId))
        }

        coVerify { pipeline.processDataItem(itemVal) }
    }
}