package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.sameInstance
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.components.AbstractEngine
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.dsl.TestingCrawlerResult
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.EventPropagation
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.NodeTester
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.test.Test


data class TestingEvent(
    override var sender: SendSource,
    override val eventName: String,
    override val propagation: EventPropagation = EventPropagation.BOTH,
    ) : Event(sender, eventName){
    override fun clone(): Event {
        return this.copy()
    }
}

class TestingEngine(
    crawlerChannelManager: CrawlerChannelManager,
) : AbstractEngine(crawlerChannelManager) {
    override fun computeResult(): CrawlerResult {
        return TestingCrawlerResult()
    }

}


class EngineTest : NodeTester<TestingEngine>() {

    lateinit var crawlerChannelManager: CrawlerChannelManager

    lateinit var downloadStub: ComponentStub

    lateinit var pipelineStub: ComponentStub

    override fun buildNode(): TestingEngine {
        return TestingEngine(crawlerChannelManager)
    }

    @BeforeEach
    fun setupStubs(): Unit = runBlocking{
        crawlerChannelManager = CrawlerChannelManager()
        crawlerChannelManager.initChannels(rootScope)
        downloadStub = spyk(ComponentStub(
            "downloadStub",
            crawlerChannelManager.downloaderChannel)
        )
        pipelineStub = spyk(ComponentStub(
            "pipelineStub",
            crawlerChannelManager.pipelineChannel
        ))
        downloadStub.start(rootScope)?.join()
        pipelineStub.start(rootScope)?.join()

        node = buildNode()
        node.start(rootScope)?.join()
    }

    @AfterEach
    fun tearDownStubs(): Unit = runBlocking {
        downloadStub.stop()
        pipelineStub.stop()
        node.stop()
        crawlerChannelManager.closeChannels()
    }


    fun getReceivedSend(stub: ComponentStub): Send{
        val captured = mutableListOf<Send>()
        coVerify { stub.processSend(capture(captured)) }
        assertThat(captured.size, equalTo(1))
        return captured[0]
    }



    @Test
    fun `send request`() = testScope.runTest {
        val url = "une url"
        val request1 = Request(sender, url)

        val data = mutableMapOf<String, Any>("result" to "1")
        downloadStub.downloadingResponses[url] = Pair(ContentType.STRING, data)

        val resp = crawlerChannelManager.spiderChannel.sendSync<DownloadingResponse>(request1)

        logger.info { "Received response: ${resp}" }

        assertThat(resp, isDownloadingResponseWith(url, data))

        val captured = getReceivedSend(downloadStub)
        assertThat(captured, isA<DownloadingRequest>(
            has(DownloadingRequest::url, equalTo(url))
        ))
    }


    @Test
    fun `send item`() = testScope.runTest {
        val item = IntDataItem(1, sender)

        val ack = crawlerChannelManager.spiderChannel.sendSync<ItemAck>(item)

        logger.info { "Received item ack on item branch: ${ack}" }
        val captured = getReceivedSend(pipelineStub)
        assertThat(
            captured, isA<IntDataItem>(has(IntDataItem::data, equalTo(item.data)))
        )
    }

    @Test
    fun `send donwloading event`() = testScope.runTest {
        val event = TestingEvent(sender, "test", EventPropagation.DOWNLOADER)

        val back = crawlerChannelManager.spiderChannel.sendSync<EventBack>(event)

        assertThat(back.send, sameInstance(event))

        coVerify(exactly = 1) { downloadStub.processSend(any()) }
        coVerify(exactly = 0) { pipelineStub.processSend(any()) }
    }

    @Test
    fun `send pipeline event`() = testScope.runTest {
        val event = TestingEvent(sender, "test", EventPropagation.PIPELINE)

        val back = crawlerChannelManager.spiderChannel.sendSync<EventBack>(event)

        assertThat(back.send, sameInstance(event))

        coVerify(exactly = 0) { downloadStub.processSend(any()) }
        coVerify(exactly = 1) { pipelineStub.processSend(any()) }
    }

    @Test
    fun `send event both`() = testScope.runTest {
        val event = TestingEvent(sender, "test", EventPropagation.BOTH)

        val back = crawlerChannelManager.spiderChannel.sendSync<EventBack>(event)

        //assertThat(back.send, sameInstance(event))

        coVerify(exactly = 1) { downloadStub.processSend(any()) }
        coVerify(exactly = 1) { pipelineStub.processSend(any()) }
    }

}


