package org.sbm4j.ktscraping.core.unit

import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractSimpleSpider
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.requests.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AbstractSpiderTest: AbstractSpiderTester() {

    val data = object:Data(){
        override fun clone(): Data {
            return this
        }
    }

    var expectedItem = DataItem.build(data, "test")

    override fun buildSpider(spiderName: String): AbstractSimpleSpider {
        return object: AbstractSimpleSpider(spiderName){
            override suspend fun parse(resp: Response) {
                this.itemsOut.send(expectedItem)
            }

            override suspend fun callbackError(ex: Throwable) {
                println("error handled by callback: ${ex.message}")
            }
        }
    }


    @Test
    fun testStartRequest() = TestScope().runTest {
        val url = "an url"

        lateinit var req: AbstractRequest
        lateinit var resp: Response
        lateinit var receivedItem: Item

        (spider as AbstractSimpleSpider).urlRequest = url

        withSpider {
            req = outChannel.receive()
            assertTrue{req.url == url}


            resp = Response(req)
            inChannel.send(resp)
            receivedItem = itemChannel.receive()
        }

        coVerify{ (spider as AbstractSimpleSpider).parse(resp)}
        assertEquals(expectedItem, receivedItem)
    }
}