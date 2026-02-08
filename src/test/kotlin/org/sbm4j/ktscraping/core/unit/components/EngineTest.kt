package org.sbm4j.ktscraping.core.unit.components

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.core.channels.ChannelFactory
import org.sbm4j.ktscraping.core.components.AbstractEngine
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.dsl.DataItemTest
import org.sbm4j.ktscraping.core.dsl.TestingCrawlerResult
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.BeforeTest
import kotlin.test.Test


class TestingEngine(
    channelFactory: ChannelFactory,
) : AbstractEngine(channelFactory) {
    override fun computeResult(): CrawlerResult {
        return TestingCrawlerResult()
    }

}


class EngineTest {

    val sender: Controllable = mockk<Controllable>()

    lateinit var channelFactory: ChannelFactory

    lateinit var engine: TestingEngine

    @BeforeTest
    fun setUp() {
        channelFactory = ChannelFactory()
        engine = TestingEngine(channelFactory)
    }

    suspend fun processDownloadEvent(event: Event){
        val ack = event.buildBack()
        channelFactory.downloaderChannel.send(ack)
    }

    suspend fun processPipelineEvent(event: Event){
        val ack = event.buildBack()
        channelFactory.pipelineChannel.send(ack)
    }

    suspend fun processDownloadingRequest(request: DownloadingRequest){
        val response = request.buildBack()
        channelFactory.downloaderChannel.send(response)
    }

    suspend fun processPipelineItem(item: Item){
        val ack = item.buildBack()
        channelFactory.pipelineChannel.send(ack)
    }

    suspend fun withEngine(
        nbMessageDownloader: Int = 1, nbMessagePipeline: Int = 1,
        func: suspend EngineTest.() -> Unit
    ) {
        channelFactory.initChannels()
        coroutineScope {
            launch {
                engine.start(this)

                logger.info { "Starting interacting with engine" }
                val start = StartEvent(sender)
                channelFactory.spiderChannel.sendSync<EventBack>(start)

                func()

                val end = EndEvent(sender)
                channelFactory.spiderChannel.sendSync<EventBack>(end)

                engine.stop()
                channelFactory.closeChannels()
                logger.debug{"finished interacting with engine"}
            }
            launch {
                channelFactory.downloaderChannel
                    .getSendFlow().take(nbMessageDownloader + 2).collect { send ->
                    when(send){
                        is Event -> processDownloadEvent(send)
                        is DownloadingRequest -> processDownloadingRequest(send)
                    }
                }
                logger.debug{"Finished receiving message on downloading branch"}
            }
            launch {
                channelFactory.pipelineChannel
                    .getSendFlow().take(nbMessagePipeline + 2).collect { send ->
                    when(send){
                        is Event -> processPipelineEvent(send)
                        is Item -> processPipelineItem(send)
                    }
                }
                logger.debug{"Finished receiving message on pipeline branch"}
            }
        }
    }


    @Test
    fun testEngineSendRequest() = TestScope().runTest {
        val request1 = Request(sender, "une url")

        withEngine(nbMessagePipeline = 0, nbMessageDownloader = 1) {
            val resp = channelFactory.spiderChannel.sendSync<DownloadingResponse>(request1)

            logger.info { "Received response: ${resp}" }
        }
    }


    @Test
    fun testEngineSendItem() = TestScope().runTest {
        val data = DataItemTest("value1", "req1")
        val item = ObjectDataItem(data, DataItemTest::class, "itemTest", sender)

        withEngine(nbMessagePipeline = 1, nbMessageDownloader = 0) {
            val ack = channelFactory.spiderChannel.sendSync<ItemAck>(item)

            logger.info { "Received item ack on item branch: ${ack}" }
        }
    }
}
