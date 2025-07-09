package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.RequestException
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemError
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Status
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

    var expectedItem = DataItem.build(data, "test")

    override fun buildSpider(spiderName: String): AbstractSpider {
        return object: AbstractSpider(spiderName){
            override suspend fun performScraping(subScope: CoroutineScope) {
                val req = Request(this, url)
                resp = sendSync(req) as DownloadingResponse
                itemsOut.send(expectedItem)
            }

        }
    }

    @Test
    fun testPerformScraping() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse
        lateinit var receivedItem: Item

        withSpider {
            req = outChannel.receive() as DownloadingRequest
            assertTrue{req.url == url}

            resp = DownloadingResponse(req)
            inChannel.send(resp)
            receivedItem = itemChannel.receive()
        }

        assertEquals(expectedItem, receivedItem)
    }

    @Test
    fun testPerformScrapingError() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse

        withSpider {
            req = outChannel.receive() as DownloadingRequest
            assertTrue { req.url == url }

            resp = DownloadingResponse(req, status = Status.ERROR)
            inChannel.send(resp)

            val error = itemChannel.receive() as ItemError
            assertIs<RequestException>(error.errorInfo.ex)
        }

    }
}