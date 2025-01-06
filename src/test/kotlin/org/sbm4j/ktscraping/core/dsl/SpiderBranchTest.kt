
package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractSimpleSpider
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

class SpiderClassTest(scope: CoroutineScope, name:String): AbstractSimpleSpider(scope, name){
    override suspend fun parse(resp: Response) {
        logger.debug { "Building a new item for request ${resp.request.name}"}
        val item = ItemTest(state["returnValue"] as String, resp.request.name, resp.request.url)

        this.itemsOut.send(item)
    }

    override suspend fun callbackError(ex: Throwable) {
    }

}

class SpiderMiddlewareClassTest(scope: CoroutineScope, name: String) : SpiderMiddleware(scope, name) {
    override fun processResponse(response: Response): Boolean {
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        return true
    }

    override fun processItem(item: Item): Item? {
        return item
    }

}

class SpiderBranchTest: CrawlerTest() {

    @Test
    fun testBuildCrawlerWithBranch() = scope.runTest{
        val expectedUrl = "une url"
        val spiderName = "Spider1"

        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                spiderBranch {
                    spiderMiddleware<SpiderMiddlewareClassTest>()
                    spider<SpiderClassTest>(spiderName){
                        urlRequest = expectedUrl
                        state["returnValue"] = name
                    }
                }
            }

            launch {
                c.start()
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request = channelFactory.spiderRequestChannel.receive()
                assertThat(request.url, equalTo(expectedUrl))

                val response = Response(request)
                channelFactory.spiderResponseChannel.send(response)

                val item = channelFactory.spiderItemChannel.receive() as ItemTest
                assertThat(item.value, equalTo(spiderName))
                logger.debug { "Received the final item" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

    }


    @Test
    fun testBuildCrawlerWithDispatcher() = scope.runTest{
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        lateinit var item1: ItemTest
        lateinit var item2: ItemTest

        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                spiderDispatcher {
                    spider<SpiderClassTest>(name = "spider1"){
                        urlRequest = url1
                        state["returnValue"] = value1
                    }
                    spider<SpiderClassTest>(name = "spider2"){
                        urlRequest = url2
                        state["returnValue"] = value2
                    }
                }
            }

            launch {
                c.start()
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request1 = channelFactory.spiderRequestChannel.receive()
                val request2 = channelFactory.spiderRequestChannel.receive()

                val response1 = Response(request1)
                val response2 = Response(request2)

                channelFactory.spiderResponseChannel.send(response1)
                channelFactory.spiderResponseChannel.send(response2)

                item1 = channelFactory.spiderItemChannel.receive() as ItemTest
                item2 = channelFactory.spiderItemChannel.receive() as ItemTest

                val itemEnd = channelFactory.spiderItemChannel.receive()

                logger.debug { "Received the final items" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(item1, isA<ItemTest>(allOf(
            has(ItemTest::url, equalTo(url1)),
            has(ItemTest::value, equalTo(value1))
        )))
        assertThat(item2, isA<ItemTest>(allOf(
            has(ItemTest::url, equalTo(url2)),
            has(ItemTest::value, equalTo(value2))
        )))
    }


    @Test
    fun testBuildCrawlerWithDispatcherAndBranch() = scope.runTest{
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        lateinit var item1: ItemTest
        lateinit var item2: ItemTest

        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                spiderBranch {
                    spiderMiddleware<SpiderMiddlewareClassTest>()
                    spiderDispatcher {
                        spiderBranch {
                            spiderMiddleware<SpiderMiddlewareClassTest>()
                            spider<SpiderClassTest>(name = "spider1"){
                                urlRequest = url1
                                state["returnValue"] = value1
                            }
                        }
                        spiderBranch {
                            spiderMiddleware<SpiderMiddlewareClassTest>()
                            spider<SpiderClassTest>(name = "spider2"){
                                urlRequest = url2
                                state["returnValue"] = value2
                            }
                        }
                    }
                }
            }

            launch {
                c.start()
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request1 = channelFactory.spiderRequestChannel.receive()
                val request2 = channelFactory.spiderRequestChannel.receive()

                val response1 = Response(request1)
                val response2 = Response(request2)

                channelFactory.spiderResponseChannel.send(response1)
                channelFactory.spiderResponseChannel.send(response2)

                item1 = channelFactory.spiderItemChannel.receive() as ItemTest
                item2 = channelFactory.spiderItemChannel.receive() as ItemTest
                val itemEnd = channelFactory.spiderItemChannel.receive()

                logger.debug { "Received the final items" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(item1, isA<ItemTest>(allOf(
            has(ItemTest::url, equalTo(url1)),
            has(ItemTest::value, equalTo(value1))
        )))
        assertThat(item2, isA<ItemTest>(allOf(
            has(ItemTest::url, equalTo(url2)),
            has(ItemTest::value, equalTo(value2))
        )))

    }
}