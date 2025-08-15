package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.SendException
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ErrorItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpiderTest: AbstractSpiderTester() {

    val url = "an url"

    lateinit var resp: DownloadingResponse

    val data = object: Data(){
        override fun clone(): Data {
            return this
        }
    }

    var expectedItem = ObjectDataItem.build(data, "test", spider)

    override fun buildSpider(spiderName: String): AbstractSpider {
        return object: AbstractSpider(spiderName){
            override suspend fun performScraping(subScope: CoroutineScope) {
                val req = Request(this, url)
                resp = sendSync(req) as DownloadingResponse
                outChannel.send(expectedItem)
            }

        }
    }

    @Test
    fun testPerformScraping() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse
        lateinit var receivedItem: Item

        withSpider {
            req = channel.channel.receive() as DownloadingRequest
            assertTrue{req.url == url}

            resp = DownloadingResponse(req)
            inChannel.send(resp)
            receivedItem = channel.channel.receive() as Item
        }

        assertEquals(expectedItem, receivedItem)
    }

    @Test
    fun testPerformScrapingError() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse

        withSpider {
            req = channel.channel.receive() as DownloadingRequest
            assertTrue { req.url == url }

            resp = DownloadingResponse(req, status = Status.ERROR)
            inChannel.send(resp)

            val error = channel.channel.receive() as ErrorItem
            assertIs<SendException>(error.errorInfo.ex)
        }

    }
}