package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck

class PipelineClassTest(name: String): AbstractPipeline(name){
    override suspend fun performAck(itemAck: ItemAck) {
    }

    override suspend fun processItem(item: Item): List<Item> {
        return listOf(item)
    }
}

class ExporterClassTest(name: String): AbstractExporter(name){
    override fun exportItem(item: Item) {
        logger.info{"Item ${item} is exported"}
    }
}

class PipelineBranchTest: CrawlerTest() {

    @Test
    fun testBuildCrawlerWithPipelineBranch() = TestScope().runTest{
        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                pipelineBranch {
                    pipeline<PipelineClassTest>()
                    exporter<ExporterClassTest>()
                }
            }

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val data1 = DataItemTest("value1", "request1")
                val item1 = DataItem.build(data1, "data1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.itemId))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }


    @Test
    fun testBuildCrawlerWithItemDispatcherAll() = TestScope().runTest{
        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                 pipelineDispatcherAll{
                    exporter<ExporterClassTest>("exporter1")
                    exporter<ExporterClassTest>("exporter2")
                }
            }

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val data1 = DataItemTest("value1", "request1")
                val item1 = DataItem.build(data1, "data1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.itemId))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }


    @Test
    fun testBuildCrawlerWithItemDispatcherOne() = TestScope().runTest{
        coroutineScope {
            val c = crawler( "MainCrawler", ::testDIModule){
                pipelineDispatcherOne("dispatcher1",
                    {item: Item ->
                        val it = item as DataItem<*>
                        val data = (it.data) as DataItemTest
                        if(data.value == "value1") itemOuts[0]
                        else itemOuts[1]
                    })
                {
                    exporter<ExporterClassTest>("exporter1")
                    exporter<ExporterClassTest>("exporter2")
                }
            }

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val data1 = DataItemTest("value1", "request1")
                val item1 = DataItem.build(data1, "data1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.itemId))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }
}