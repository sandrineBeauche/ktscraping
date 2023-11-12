package org.sbm4j.ktscraping.core.utils

import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbstractSpiderTest: ScrapingTest<Response, Request>(){

    lateinit var crawler: Crawler

    lateinit var spider: AbstractSpider

    val itemChannel: Channel<Item> = Channel<Item>(Channel.UNLIMITED)

    var expectedItem = object: Item {}

    val spiderName: String = "Spider"

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        crawler = mockk<Crawler>()

        spider = spyk(object: AbstractSpider(crawler.scope, spiderName){
            override suspend fun parse(req: Request, resp: Response) {
                this.itemsOut.send(expectedItem)
            }

            override suspend fun callbackError(req: Request, resp: Response) {
                TODO("Not yet implemented")
            }
        })

        every { spider.requestOut } returns outChannel
        every { spider.responseIn } returns inChannel
        every { spider.itemsOut } returns itemChannel
    }

    @Test
    fun testStartRequest() = TestScope().runTest {
        val url = "an url"

        var req: Request
        var resp: Response
        var receivedItem: Item

        coroutineScope {
            every { spider.scope } returns this
            every { spider.urlRequest } returns url

            spider.start()

            req = outChannel.receive()
            assertTrue{req.url == url}

            resp = Response(req)
            inChannel.send(resp)
            receivedItem = itemChannel.receive()

            outChannel.close()
            inChannel.close()
            itemChannel.close()
        }

        coVerify{ spider.parse(req, resp)}
        assertEquals(expectedItem, receivedItem)
    }
}