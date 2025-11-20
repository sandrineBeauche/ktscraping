package org.sbm4j.ktscraping.core.unit.components

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.SendException
import org.sbm4j.ktscraping.core.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpiderTest: AbstractSpiderTester() {

    val url = "an url"

    lateinit var resp: DownloadingResponse

    val data = object: Data(){
        override fun clone(): Data {
            return this
        }
    }



    override fun buildSpider(spiderName: String): AbstractSpider {
        return object: AbstractSpider(spiderName){
            override suspend fun performScraping(subScope: CoroutineScope) {
                val req = Request(this, url)
                resp = sendSync(req) as DownloadingResponse

                logger.debug{"${name}: received a response and send an item"}
                val expectedItem = ObjectDataItem.Companion.build(data, "test", this)
                outChannel.send(expectedItem)
            }

        }
    }

    @Test
    fun testPerformScraping() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse
        lateinit var receivedItem: Item

        withSpider(2) { send ->
            when(send){
                is DownloadingRequest -> {
                    logger.debug{"Received the request ${send}"}
                    assertTrue { send.url == url }
                    resp = send.buildBack()
                    outChannel.send(resp)
                }
                is ObjectDataItem<*> -> {
                    logger.debug{"received an item: ${receivedItem}"}
                    receivedItem = send
                }
            }
        }

        val receivedData = (receivedItem as ObjectDataItem<*>).data
        assertEquals(receivedData, data)
    }

    @Test
    fun testPerformScrapingError() = TestScope().runTest {
        lateinit var req: AbstractRequest
        lateinit var resp: DownloadingResponse

        val errorControllable = mockk<Controllable>()

        lateinit var receivedError: ErrorInternal

        withSpider(2) { send ->
            when(send){
                is DownloadingRequest -> {
                    logger.debug{"Received the request ${send}"}
                    assertTrue { send.url == url }
                    val error = ErrorInfo(Exception("an exception"), errorControllable, ErrorLevel.MAJOR)
                    resp = send.buildErrorBack(error)
                    outChannel.send(resp)
                }
                is ErrorInternal -> {
                    logger.debug{"Received internal error ${send}"}
                    receivedError = send
                }
            }
        }

        val r = (receivedError.errorInfo.ex as SendException).resp
        assertSame(errorControllable, r.errorInfos[0].controllable)
    }
}