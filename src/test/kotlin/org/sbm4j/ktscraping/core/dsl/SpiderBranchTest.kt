
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
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Response

class SpiderClassTest(name:String): AbstractSimpleSpider(name){
    override suspend fun parse(resp: Response) {
        logger.debug { "Building a new item for request ${resp.request.name}"}
        val data = DataItemTest(state["returnValue"] as String, resp.request.name, resp.request.url)

        this.itemsOut.send(DataItem.build(data, "itemTest"))
    }

    override suspend fun callbackError(ex: Throwable) {
    }

}

class SpiderMiddlewareClassTest(name: String) : SpiderMiddleware(name) {
    override suspend fun processResponse(response: Response): Boolean {
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        return true
    }

    override suspend fun processItem(item: Item): List<Item> {
        return listOf(item)
    }

}

class SpiderBranchTest: CrawlerTest() {

    @Test
    fun testBuildCrawlerWithBranch() = TestScope().runTest{
        val expectedUrl = "une url"
        val spiderName = "Spider1"

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                spiderBranch {
                    spiderMiddleware<SpiderMiddlewareClassTest>()
                    spider<SpiderClassTest>(spiderName){
                        urlRequest = expectedUrl
                        state["returnValue"] = name
                    }
                }
            }

            launch {
                c.start(this)
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request = channelFactory.spiderRequestChannel.receive()
                assertThat(request.url, equalTo(expectedUrl))

                val response = Response(request)
                channelFactory.spiderResponseChannel.send(response)

                val item = channelFactory.spiderItemChannel.receive() as DataItem<*>
                val data = item.data as DataItemTest
                assertThat(data.value, equalTo(spiderName))
                logger.debug { "Received the final item" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

    }


    @Test
    fun testBuildCrawlerWithDispatcher() = TestScope().runTest{
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        lateinit var item1: DataItemTest
        lateinit var item2: DataItemTest

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
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
                c.start(this)
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request1 = channelFactory.spiderRequestChannel.receive()
                val request2 = channelFactory.spiderRequestChannel.receive()

                val response1 = Response(request1)
                val response2 = Response(request2)

                channelFactory.spiderResponseChannel.send(response1)
                channelFactory.spiderResponseChannel.send(response2)

                item1 = (channelFactory.spiderItemChannel.receive() as DataItem<*>).data as DataItemTest
                item2 = (channelFactory.spiderItemChannel.receive() as DataItem<*>).data as DataItemTest

                val itemEnd = channelFactory.spiderItemChannel.receive()

                logger.debug { "Received the final items" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(item1, isA<DataItemTest>(allOf(
            has(DataItemTest::url, equalTo(url1)),
            has(DataItemTest::value, equalTo(value1))
        )))
        assertThat(item2, isA<DataItemTest>(allOf(
            has(DataItemTest::url, equalTo(url2)),
            has(DataItemTest::value, equalTo(value2))
        )))
    }


    @Test
    fun testBuildCrawlerWithDispatcherAndBranch() = TestScope().runTest{
        val url1 = "une url 1"
        val url2 = "une url 2"
        val value1 = "value1"
        val value2 = "value2"

        lateinit var item1: DataItemTest
        lateinit var item2: DataItemTest

        coroutineScope {
            val c = crawler( "MainCrawler", ::testDIModule){
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
                c.start(this)
            }
            launch{
                logger.debug { "interacting with crawler" }
                val request1 = channelFactory.spiderRequestChannel.receive()
                val request2 = channelFactory.spiderRequestChannel.receive()

                val response1 = Response(request1)
                val response2 = Response(request2)

                channelFactory.spiderResponseChannel.send(response1)
                channelFactory.spiderResponseChannel.send(response2)

                item1 = (channelFactory.spiderItemChannel.receive() as DataItem<*>).data as DataItemTest
                item2 = (channelFactory.spiderItemChannel.receive() as DataItem<*>).data as DataItemTest
                val itemEnd = channelFactory.spiderItemChannel.receive()

                logger.debug { "Received the final items" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(item1, isA<DataItemTest>(allOf(
            has(DataItemTest::url, equalTo(url1)),
            has(DataItemTest::value, equalTo(value1))
        )))
        assertThat(item2, isA<DataItemTest>(allOf(
            has(DataItemTest::url, equalTo(url2)),
            has(DataItemTest::value, equalTo(value2))
        )))

    }
}