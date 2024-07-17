package org.sbm4j.ktscraping.core.unit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.coVerify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.dsl.ItemTest
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemStatus

class AbstractPipelineTest: AbstractPipelineTester() {
    override fun buildPipeline(sc: CoroutineScope, pipelineName: String): AbstractPipeline {
        return object: AbstractPipeline(sc, pipelineName){
            override fun processItem(item: Item): Item? {
                return item
            }
        }
    }


    @Test
    fun testPipeline() = TestScope().runTest {

        val itemVal = ItemTest("coucou", "request1")

        withPipeline {
            inChannel.send(itemVal)
            val processed = followInChannel.receive()

            val ack = ItemAck(processed.id, ItemStatus.PROCESSED)
            outChannel.send(ack)
            val receivedAck = followOutChannel.receive()

            assertThat(receivedAck.itemId, equalTo(itemVal.id))
        }

        coVerify { pipeline.processItem(itemVal) }
    }
}