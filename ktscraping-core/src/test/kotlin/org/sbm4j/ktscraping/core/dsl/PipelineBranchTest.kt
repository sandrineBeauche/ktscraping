package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.utils.DataItemTest
import org.sbm4j.ktscraping.core.utils.isOKEndItemAck
import org.sbm4j.ktscraping.core.utils.isOKStartItemAck
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.meercat.nodes.logger

class PipelineClassTest(name: String): AbstractPipeline(name){
}

class ExporterClassTest(name: String): AbstractExporter(name){
    override suspend fun exportItem(item: Item) {
        logger.info{"Item ${item} is exported"}
    }
}



class PipelineBranchTest: CrawlerTest() {


    @Test
    fun `branch with pipeline and exporter`() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineBranch {
                pipeline<PipelineClassTest>()
                exporter<ExporterClassTest>()
            }
        }

        c.start(this)?.join()

        logger.debug { "interacting with crawler" }
        sendStartEvent(crawlerChannelManager.pipelineChannel)

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1", sender)
        val ack = crawlerChannelManager.pipelineChannel.sendSync<ItemAck>(item1)

        assertThat(ack.send.channelableId, equalTo(item1.channelableId))

        sendEndEvent(crawlerChannelManager.pipelineChannel)
        c.stop()

    }


    @Test
    fun `branch with dispatcher all and exporters`() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineDispatcherAll {
                exporter<ExporterClassTest>("exporter1")
                exporter<ExporterClassTest>("exporter2")
            }
        }

        c.start(this)?.join()

        logger.debug { "interacting with crawler" }
        sendStartEvent(crawlerChannelManager.pipelineChannel)

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1", sender)
        val ack = crawlerChannelManager.pipelineChannel.sendSync<ItemAck>(item1)

        assertThat(ack.send.channelableId, equalTo(item1.channelableId))

        sendEndEvent(crawlerChannelManager.pipelineChannel)
        c.stop()
    }


    @Test
    fun `branch with dispatcher one and exporters`() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineDispatcherOne(
                "dispatcher1",
                { item: Item ->
                    val it = item as ObjectDataItem<*>
                    val data = (it.data) as DataItemTest
                    if (data.value == "value1") channelOuts[0]
                    else channelOuts[1]
                })
            {
                exporter<ExporterClassTest>("exporter1")
                exporter<ExporterClassTest>("exporter2")
            }
        }


        c.start(this)?.join()

        logger.debug { "interacting with crawler" }
        sendStartEvent(crawlerChannelManager.pipelineChannel)

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1", sender)
        val ack = crawlerChannelManager.pipelineChannel.sendSync<ItemAck>(item1)

        assertThat(ack.send.channelableId, equalTo(item1.channelableId))

        sendEndEvent(crawlerChannelManager.pipelineChannel)
        c.stop()
    }
}