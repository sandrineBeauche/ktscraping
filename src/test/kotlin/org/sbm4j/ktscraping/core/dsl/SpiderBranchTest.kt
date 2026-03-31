
package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSimpleSpider
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.utils.DataItemTest
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.logger

class TestingSpiderClass(name:String): AbstractSimpleSpider(name){
    override suspend fun parse(resp: DownloadingResponse) {
        logger.debug { "Building a new item for request ${resp.send.name}"}
        val req = resp.send
        val value = state["returnValue"] as String
        val data = DataItemTest(value, req.name, req.url)

        this.outChannel.send(ObjectDataItem.build(data, "itemTest", this))
    }

    override suspend fun callbackError(ex: Throwable) {
    }
}

class TestingSpiderMiddlewareClass(name: String) : SpiderMiddleware(name){
    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }
}


class SpiderBranchTest: CrawlerTest() {


    suspend fun answerStartEvent(event: StartEvent, channel: SuperChannel) {
        logger.debug{ "Received starting event"}
        val startResp = event.buildBack()
        channel.send(startResp)
    }

    suspend fun answerEndEvent(event: EndEvent, channel: SuperChannel) {
        logger.debug{ "Received ending event request"}
        val endResp = event.buildBack()
        channel.send(endResp)
    }

    @Test
    fun testBuildCrawlerWithBranch() = TestScope().runTest {
        val expectedUrl = "une url"
        val spiderName = "Spider1"

        val c = crawler("MainCrawler", ::testDIModule) {
            spiderBranch {
                spiderMiddleware<TestingSpiderMiddlewareClass>()
                spider<TestingSpiderClass>(spiderName) {
                    urlRequest = expectedUrl
                    state["returnValue"] = name
                }
            }
        }

        val spiderChannel = c.channelManager.spiderChannel

        coroutineScope {
            c.channelManager.initChannels(this)

            launch {
                val job = c.start(this)
                job?.join()
                c.waitFinished()
                c.stop()
            }
            launch{
                logger.debug { "interacting with crawler" }
                spiderChannel.getSendFlow().take(4).collect{ send ->
                    when(send){
                        is StartEvent -> answerStartEvent(send, spiderChannel)
                        is EndEvent -> answerEndEvent(send, spiderChannel)
                        is DownloadingRequest -> {
                            logger.debug { "Received the request: $send" }
                            assertThat(send.url, equalTo(expectedUrl))
                            val response = send.buildBack()
                            spiderChannel.send(response)
                        }
                        is ObjectDataItem<*> -> {
                            val data = send.data as DataItemTest
                            assertThat(data.value, equalTo(spiderName))
                            logger.debug { "Received the final item: $data" }
                            val ack = send.buildBack()
                            spiderChannel.send(ack)
                        }
                    }
                }
            }
        }

    }


    @Test
    fun testBuildCrawlerWithDispatcher() = TestScope().runTest {
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        val c = crawler("MainCrawler", ::testDIModule) {
            spiderDispatcher {
                spider<TestingSpiderClass>(name = "spider1") {
                    urlRequest = url1
                    state["returnValue"] = value1
                }
                spider<TestingSpiderClass>(name = "spider2") {
                    urlRequest = url2
                    state["returnValue"] = value2
                }
            }
        }

        c.start(this)

        logger.debug { "interacting with crawler" }
        //answerStartEvent()

        val request1 = crawlerChannelManager.spiderChannel.receiveSend<DownloadingRequest>()
        val request2 = crawlerChannelManager.spiderChannel.receiveSend<DownloadingRequest>()

        val response1 = DownloadingResponse(request1)
        val response2 = DownloadingResponse(request2)

        crawlerChannelManager.spiderChannel.send(response1)
        crawlerChannelManager.spiderChannel.send(response2)

        val item1: DataItemTest = (crawlerChannelManager.spiderChannel.receiveSend<ObjectDataItem<*>>()).data as DataItemTest
        val item2: DataItemTest = (crawlerChannelManager.spiderChannel.receiveSend<ObjectDataItem<*>>()).data as DataItemTest

        logger.debug { "Received the final items:\n $item1 \n $item2" }

        //answerEndEvent()
        c.waitFinished()
        c.stop()
        crawlerChannelManager.closeChannels()

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
                spiderMiddleware<TestingSpiderMiddlewareClass>()
                spiderDispatcher {
                    spiderBranch {
                        spiderMiddleware<TestingSpiderMiddlewareClass>()
                        spider<TestingSpiderClass>(name = "spider1") {
                            urlRequest = url1
                            state["returnValue"] = value1
                        }
                    }
                    spiderBranch {
                        spiderMiddleware<TestingSpiderMiddlewareClass>()
                        spider<TestingSpiderClass>(name = "spider2") {
                            urlRequest = url2
                            state["returnValue"] = value2
                        }
                    }
                }
            }
        }


        c.start(this)

        logger.debug { "interacting with crawler" }
        //answerStartEvent()
        val request1 = crawlerChannelManager.spiderChannel.receiveSend<DownloadingRequest>()
        val request2 = crawlerChannelManager.spiderChannel.receiveSend<DownloadingRequest>()

        val response1 = DownloadingResponse(request1)
        val response2 = DownloadingResponse(request2)

        crawlerChannelManager.spiderChannel.send(response1)
        crawlerChannelManager.spiderChannel.send(response2)

        item1 = (crawlerChannelManager.spiderChannel.receiveSend<ObjectDataItem<*>>()).data as DataItemTest
        item2 = (crawlerChannelManager.spiderChannel.receiveSend<ObjectDataItem<*>>()).data as DataItemTest
        logger.debug { "Received the final items:\n $item1 \n $item2" }

        //answerEndEvent()
        c.waitFinished()
        c.stop()
        crawlerChannelManager.closeChannels()


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