package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.RequestException
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.requests.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpiderTest: AbstractSpiderTester() {

    val url = "an url"

    lateinit var resp: Response

    var expectedItem = object: AbstractItem(){
        override fun clone(): Item {
            return this
        }
    }

    override fun buildSpider(sc: CoroutineScope, spiderName: String): AbstractSpider {
        return object: AbstractSpider(sc, spiderName){
            override suspend fun performScraping() {
                val req = Request(this, url)
                resp = sendSync(req)
                itemsOut.send(expectedItem)
            }

        }
    }

    @Test
    fun testPerformScraping() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: Response
        lateinit var receivedItem: Item

        withSpider {
            req = outChannel.receive()
            assertTrue{req.url == url}

            resp = Response(req)
            inChannel.send(resp)
            receivedItem = itemChannel.receive()
        }

        assertEquals(expectedItem, receivedItem)
    }

    @Test
    fun testPerformScrapingError() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: Response

        assertThrows<RequestException> {
            withSpider {
                req = outChannel.receive()
                assertTrue { req.url == url }

                resp = Response(req, Status.ERROR)
                inChannel.send(resp)
            }
        }
    }
}