
package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isA
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSimpleSpider
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.DataItemTest
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
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
        val item = ObjectDataItem.build(data, "itemTest", this)

        this.outChannel.sendSync<ItemAck>(item)
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


    @Test
    fun `branch with spider and middleware`() = TestScope().runTest {
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

        val stub = spyk(ComponentStub(
            "stub", c.channelManager.spiderChannel))
        stub.downloadingResponses[expectedUrl] =
            Pair(ContentType.STRING, mutableMapOf("result" to "value3"))
        c.topologyManager.nodes.add(stub)

        c.start(this)?.join()
        c.waitFinished()
        c.stop()

        coVerify(exactly = 1) { stub.performRequest(any()) }
        coVerify(exactly = 1) { stub.processItem(any()) }
    }


    @Test
    fun `branch with spiders and dispatcher `() = TestScope().runTest {
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

        val stub = spyk(ComponentStub(
            "stub", c.channelManager.spiderChannel))
        stub.downloadingResponses[url1] =
            Pair(ContentType.STRING, mutableMapOf("result" to "value3"))
        stub.downloadingResponses[url2] =
            Pair(ContentType.STRING, mutableMapOf("result" to "value4"))
        c.topologyManager.nodes.add(stub)

        c.start(this)?.join()
        c.waitFinished()
        c.stop()

        val items: MutableList<Item> = mutableListOf()
        coVerify { stub.processItem(capture(items)) }

        assertThat(items, hasSize(equalTo(2)))
    }


    @Test
    fun testBuildCrawlerWithDispatcherAndBranch() = TestScope().runTest {
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"


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

        val stub = spyk(ComponentStub(
            "stub", c.channelManager.spiderChannel))
        stub.downloadingResponses[url1] =
            Pair(ContentType.STRING, mutableMapOf("result" to "value3"))
        stub.downloadingResponses[url2] =
            Pair(ContentType.STRING, mutableMapOf("result" to "value4"))
        c.topologyManager.nodes.add(stub)

        c.start(this)?.join()
        c.waitFinished()
        c.stop()

        val items: MutableList<Item> = mutableListOf()
        coVerify { stub.processItem(capture(items)) }

        assertThat(items, hasSize(equalTo(2)))
    }
}