package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.utils.isOkItemAck
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test

class TestingSpiderMiddleware() : SpiderMiddleware("middleware"){
    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        request.url = "another url"
        return true
    }

    override suspend fun processItem(item: Item): Any? {
        (item as IntDataItem).data++
        return item
    }
}

class SpiderMiddlewareTests: AbstractSpiderMiddlewareTester<TestingSpiderMiddleware>() {
    override fun buildNode(): TestingSpiderMiddleware {
        val result = TestingSpiderMiddleware()
        result.inChannel = inChannel
        result.outChannel = outChannel
        return result
    }

    @Test
    fun `forward request and response for request and item`() = testScope.runTest {
        val url1 = "an url"
        val url2 = "another url"

        val cs = (stub as ComponentStub)
        val data = mutableMapOf<String, Any>("result" to "1")
        cs.downloadingResponses[url2] = Pair(ContentType.STRING, data)

        val req = Request(sender, url1)
        lateinit var resp: DownloadingResponse
        lateinit var ack: ItemAck
        lateinit var item: IntDataItem

        withConsumer {
            resp = inChannel.sendSync<DownloadingResponse>(req)
            val intData = resp.contents["result"] as String
            item = IntDataItem(intData.toInt(), sender)
            ack = inChannel.sendSync<ItemAck>(item)
        }

        val captured = getReceivedSend(cs)
        assertThat(captured, hasSize(equalTo(4)))
        assertThat(captured[1], isA<Request>(
            has(Request::url, equalTo(url2))
        ))
        assertThat(resp, isDownloadingResponseWith(url2, data))
        assertThat(captured[2], isA<IntDataItem>(
            has(IntDataItem::data, equalTo(2))
        ))
        assertThat(ack, isOkItemAck(item))
    }
}