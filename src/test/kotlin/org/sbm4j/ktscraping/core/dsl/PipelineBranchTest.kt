package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.isOKEndItemAck
import org.sbm4j.ktscraping.core.utils.isOKStartItemAck
import org.sbm4j.ktscraping.data.item.*

class PipelineClassTest(name: String): AbstractPipeline(name){
}

class ExporterClassTest(name: String): AbstractExporter(name){
    override fun exportItem(item: DataItem<*>) {
        logger.info{"Item ${item} is exported"}
    }
}



class PipelineBranchTest: CrawlerTest() {

    suspend fun sendStartItem(){
        val startItem = StartItem()
        channelFactory.itemChannel.send(startItem)
        val startAck = channelFactory.itemAckChannel.receive() as EventItemAck
        logger.info{ "received ack for the start event item " }

        assertThat(startAck, isOKStartItemAck())
    }


    suspend fun sendEndItem(){
        val endItem = EndItem()
        channelFactory.itemChannel.send(endItem)
        val endAck = channelFactory.itemAckChannel.receive() as EventItemAck
        logger.info{ "received ack for the end event item " }

        assertThat(endAck, isOKEndItemAck())
    }

    @Test
    fun testBuildCrawlerWithPipelineBranch() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineBranch {
                pipeline<PipelineClassTest>()
                exporter<ExporterClassTest>()
            }
        }

        c.start(this)

        logger.debug { "interacting with crawler" }
        sendStartItem()

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1")
        channelFactory.itemChannel.send(item1)

        val ack = channelFactory.itemAckChannel.receive()
        assertThat(ack.channelableId, equalTo(item1.channelableId))

        sendEndItem()
        c.stop()
        channelFactory.closeChannels()

    }


    @Test
    fun testBuildCrawlerWithItemDispatcherAll() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineDispatcherAll {
                exporter<ExporterClassTest>("exporter1")
                exporter<ExporterClassTest>("exporter2")
            }
        }

        c.start(this)

        logger.debug { "interacting with crawler" }
        sendStartItem()

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1")
        channelFactory.itemChannel.send(item1)

        val ack = channelFactory.itemAckChannel.receive()
        assertThat(ack.channelableId, equalTo(item1.channelableId))

        sendEndItem()
        c.stop()
        channelFactory.closeChannels()

    }


    @Test
    fun testBuildCrawlerWithItemDispatcherOne() = TestScope().runTest {

        val c = crawler("MainCrawler", ::testDIModule) {
            pipelineDispatcherOne(
                "dispatcher1",
                { item: Item ->
                    val it = item as ObjectDataItem<*>
                    val data = (it.data) as DataItemTest
                    if (data.value == "value1") itemOuts[0]
                    else itemOuts[1]
                })
            {
                exporter<ExporterClassTest>("exporter1")
                exporter<ExporterClassTest>("exporter2")
            }
        }


        c.start(this)

        logger.debug { "interacting with crawler" }
        sendStartItem()

        val data1 = DataItemTest("value1", "request1")
        val item1 = ObjectDataItem.build(data1, "data1")
        channelFactory.itemChannel.send(item1)

        val ack = channelFactory.itemAckChannel.receive()
        assertThat(ack.channelableId, equalTo(item1.channelableId))

        sendEndItem()
        c.stop()
        channelFactory.closeChannels()
    }
}