
package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractSimpleSpider
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse

class SpiderClassTest(name:String): AbstractSimpleSpider(name){
    override suspend fun parse(resp: DownloadingResponse) {
        logger.debug { "Building a new item for request ${resp.request.name}"}
        val req = resp.request
        val data = DataItemTest(state["returnValue"] as String, req.name, req.url)

        this.itemsOut.send(ObjectDataItem.build(data, "itemTest"))
    }

    override suspend fun callbackError(ex: Throwable) {
    }
}

class SpiderMiddlewareClassTest(name: String) : SpiderMiddleware(name) {
    override suspend fun processResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        return true
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }

    override suspend fun processDataItem(item: ObjectDataItem<*>): List<Item> {
        return listOf(item)
    }

}

class SpiderBranchTest: CrawlerTest() {


    suspend fun answerStartRequestEvent(){
        val startReq = channelFactory.spiderRequestChannel.receive() as StartRequest
        logger.debug{ "Received starting event request"}
        val startResp = EventResponse(startReq.eventName, startReq, Status.OK)
        channelFactory.spiderResponseChannel.send(startResp)
    }

    suspend fun answerEndEvent(){
        val endReq = channelFactory.spiderRequestChannel.receive() as EndRequest
        logger.debug{ "Received ending event request"}
        val endResp = EventResponse(endReq.eventName, endReq, Status.OK)
        channelFactory.spiderResponseChannel.send(endResp)
    }

    @Test
    fun testBuildCrawlerWithBranch() = TestScope().runTest {
        val expectedUrl = "une url"
        val spiderName = "Spider1"

        val c = crawler("MainCrawler", ::testDIModule) {
            spiderBranch {
                spiderMiddleware<SpiderMiddlewareClassTest>()
                spider<SpiderClassTest>(spiderName) {
                    urlRequest = expectedUrl
                    state["returnValue"] = name
                }
            }
        }

        c.start(this)

        logger.debug { "interacting with crawler" }
        answerStartRequestEvent()

        val request = channelFactory.spiderRequestChannel.receive() as DownloadingRequest
        assertThat(request.url, equalTo(expectedUrl))

        val response = DownloadingResponse(request)
        channelFactory.spiderResponseChannel.send(response)

        val item = channelFactory.spiderItemChannel.receive() as ObjectDataItem<*>
        val data = item.data as DataItemTest
        assertThat(data.value, equalTo(spiderName))
        logger.debug { "Received the final item: $data" }

        answerEndEvent()
        c.waitFinished()
        c.stop()
        channelFactory.closeChannels()
    }


    @Test
    fun testBuildCrawlerWithDispatcher() = TestScope().runTest {
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        val c = crawler("MainCrawler", ::testDIModule) {
            spiderDispatcher {
                spider<SpiderClassTest>(name = "spider1") {
                    urlRequest = url1
                    state["returnValue"] = value1
                }
                spider<SpiderClassTest>(name = "spider2") {
                    urlRequest = url2
                    state["returnValue"] = value2
                }
            }
        }

        c.start(this)

        logger.debug { "interacting with crawler" }
        answerStartRequestEvent()

        val request1 = channelFactory.spiderRequestChannel.receive()
        val request2 = channelFactory.spiderRequestChannel.receive()

        val response1 = DownloadingResponse(request1 as DownloadingRequest)
        val response2 = DownloadingResponse(request2 as DownloadingRequest)

        channelFactory.spiderResponseChannel.send(response1)
        channelFactory.spiderResponseChannel.send(response2)

        val item1: DataItemTest = (channelFactory.spiderItemChannel.receive() as ObjectDataItem<*>).data as DataItemTest
        val item2: DataItemTest = (channelFactory.spiderItemChannel.receive() as ObjectDataItem<*>).data as DataItemTest

        logger.debug { "Received the final items:\n $item1 \n $item2" }

        answerEndEvent()
        c.waitFinished()
        c.stop()
        channelFactory.closeChannels()


        assertThat(
            item1, isA<DataItemTest>(
                allOf(
                    has(DataItemTest::url, equalTo(url1)),
                    has(DataItemTest::value, equalTo(value1))
                )
            )
        )
        assertThat(
            item2, isA<DataItemTest>(
                allOf(
                    has(DataItemTest::url, equalTo(url2)),
                    has(DataItemTest::value, equalTo(value2))
                )
            )
        )
    }


    @Test
    fun testBuildCrawlerWithDispatcherAndBranch() = TestScope().runTest {
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        lateinit var item1: DataItemTest
        lateinit var item2: DataItemTest


        val c = crawler("MainCrawler", ::testDIModule) {
            spiderBranch {
                spiderMiddleware<SpiderMiddlewareClassTest>()
                spiderDispatcher {
                    spiderBranch {
                        spiderMiddleware<SpiderMiddlewareClassTest>()
                        spider<SpiderClassTest>(name = "spider1") {
                            urlRequest = url1
                            state["returnValue"] = value1
                        }
                    }
                    spiderBranch {
                        spiderMiddleware<SpiderMiddlewareClassTest>()
                        spider<SpiderClassTest>(name = "spider2") {
                            urlRequest = url2
                            state["returnValue"] = value2
                        }
                    }
                }
            }
        }


        c.start(this)

        logger.debug { "interacting with crawler" }
        answerStartRequestEvent()
        val request1 = channelFactory.spiderRequestChannel.receive()
        val request2 = channelFactory.spiderRequestChannel.receive()

        val response1 = DownloadingResponse(request1 as DownloadingRequest)
        val response2 = DownloadingResponse(request2 as DownloadingRequest)

        channelFactory.spiderResponseChannel.send(response1)
        channelFactory.spiderResponseChannel.send(response2)

        item1 = (channelFactory.spiderItemChannel.receive() as ObjectDataItem<*>).data as DataItemTest
        item2 = (channelFactory.spiderItemChannel.receive() as ObjectDataItem<*>).data as DataItemTest
        logger.debug { "Received the final items:\n $item1 \n $item2" }

        answerEndEvent()
        c.waitFinished()
        c.stop()
        channelFactory.closeChannels()


        assertThat(
            item1, isA<DataItemTest>(
                allOf(
                    has(DataItemTest::url, equalTo(url1)),
                    has(DataItemTest::value, equalTo(value1))
                )
            )
        )
        assertThat(
            item2, isA<DataItemTest>(
                allOf(
                    has(DataItemTest::url, equalTo(url2)),
                    has(DataItemTest::value, equalTo(value2))
                )
            )
        )

    }
}