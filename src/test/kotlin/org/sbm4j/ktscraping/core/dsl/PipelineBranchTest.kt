package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck

class PipelineClassTest(scope: CoroutineScope, name: String): AbstractPipeline(scope, name){
    override suspend fun performAck(itemAck: ItemAck) {
    }

    override fun processItem(item: Item): Item? {
        return item
    }
}

class ExporterClassTest(scope: CoroutineScope, name: String): AbstractExporter(scope, name){
    override fun exportItem(item: Item) {
        logger.info{"Item ${item} is exported"}
    }
}

class PipelineBranchTest: CrawlerTest() {

    @Test
    fun testBuildCrawlerWithPipelineBranch() = scope.runTest{
        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                pipelineBranch {
                    pipeline<PipelineClassTest>()
                    exporter<ExporterClassTest>()
                }
            }

            launch {
                c.start()
            }
            launch {
                logger.debug { "interacting with crawler" }

                val item1 = ItemTest("value1", "request1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.id))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }


    @Test
    fun testBuildCrawlerWithItemDispatcherAll() = scope.runTest{
        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                 pipelineDispatcherAll{
                    exporter<ExporterClassTest>("exporter1")
                    exporter<ExporterClassTest>("exporter2")
                }
            }

            launch {
                c.start()
            }
            launch {
                logger.debug { "interacting with crawler" }

                val item1 = ItemTest("value1", "request1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.id))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }


    @Test
    fun testBuildCrawlerWithItemDispatcherOne() = scope.runTest{
        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                pipelineDispatcherOne("dispatcher1",
                    {item: Item ->
                        val it = item as ItemTest
                        if(it.value == "value1") itemOuts[0]
                        else itemOuts[1]
                    })
                {
                    exporter<ExporterClassTest>("exporter1")
                    exporter<ExporterClassTest>("exporter2")
                }
            }

            launch {
                c.start()
            }
            launch {
                logger.debug { "interacting with crawler" }

                val item1 = ItemTest("value1", "request1")
                channelFactory.itemChannel.send(item1)

                val ack = channelFactory.itemAckChannel.receive()
                assertThat(ack.itemId, equalTo(item1.id))

                c.stop()
                channelFactory.closeChannels()
            }
        }
    }
}