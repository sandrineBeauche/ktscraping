package org.sbm4j.ktscraping.core.unit

import io.mockk.coVerify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.requests.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AbstractSpiderTest: AbstractSpiderTester() {


    var expectedItem = object: AbstractItem(){
        override fun clone(): Item {
            return this
        }
    }

    override fun buildSpider(sc: CoroutineScope, spiderName: String): AbstractSpider{
        return object: AbstractSpider(sc,  spiderName){
            override suspend fun parse(req: AbstractRequest, resp: Response) {
                this.itemsOut.send(expectedItem)
            }

            override suspend fun callbackError(req: AbstractRequest, resp: Response) {
                TODO("Not yet implemented")
            }
        }
    }


    @Test
    fun testStartRequest() = TestScope().runTest {
        val url = "an url"

        lateinit var req: AbstractRequest
        lateinit var resp: Response
        lateinit var receivedItem: Item

        spider.urlRequest = url

        withSpider {
            req = outChannel.receive()
            assertTrue{req.url == url}

            resp = Response(req)
            inChannel.send(resp)
            receivedItem = itemChannel.receive()
        }

        coVerify{ spider.parse(req, resp)}
        assertEquals(expectedItem, receivedItem)
    }
}