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
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemStatus

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
        val itemVal = DataItem.build(dataVal, "itemTest")

        withPipeline {
            inChannel.send(itemVal)
            val processed = followInChannel.receive()

            val ack = ItemAck(processed.itemId, ItemStatus.PROCESSED)
            outChannel.send(ack)
            val receivedAck = followOutChannel.receive()

            assertThat(receivedAck.itemId, equalTo(itemVal.itemId))
        }

        coVerify { pipeline.processItem(itemVal) }
    }
}