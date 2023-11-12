package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kodein.di.*
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response


class SpiderMiddlewareClassTest(scope: CoroutineScope, name: String) : SpiderMiddleware(scope, name) {
    override fun processResponse(response: Response): Boolean {
        return true
    }

    override fun processRequest(request: Request): Any? {
        return request
    }

    override fun processItem(item: Item): Boolean {
        return true
    }

}

data class ItemTest(val value: String): Item

class SpiderClasstest(scope: CoroutineScope, name:String): AbstractSpider(scope, name){
    override suspend fun parse(req: Request, resp: Response) {
        logger.debug { "Building a new item "}
        val item = ItemTest(name)
        this.itemsOut.send(item)
    }

    override suspend fun callbackError(req: Request, resp: Response) {
    }

}

class EmptyTestingCrawler(
    scope: CoroutineScope,
    name: String = "TestCrawler",
    channelFactory: ChannelFactory,
    override val di: DI
) : AbstractCrawler(scope, name, channelFactory){
    override suspend fun start() {
        logger.info{"Starting testing crawler ${name}"}
        super.start()
    }

    override suspend fun stop() {
        logger.info{"Stopping testing crawler ${name}"}
        super.stop()
    }
}


class CrawlerTest {

    val scope = TestScope()

    val channelFactory : ChannelFactory = ChannelFactory()

    fun testDIModule(scope: CoroutineScope, name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bindSingleton<CoroutineScope> { scope }
            bind<Crawler> { multiton {di: DI -> EmptyTestingCrawler(instance(), name, instance(), di) }}
            bindSingleton<ChannelFactory> { channelFactory }
        }
        return mod
    }

    @Test
    fun testBuildCrawlerWithBranch() = scope.runTest{
        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                spiderBranch {
                    spiderMiddleware(SpiderMiddlewareClassTest::class) {
                        state["arg1"] = "value1"
                    }
                    spider(SpiderClasstest::class){
                        urlRequest = "une url"
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
                val response = Response(request)
                channelFactory.spiderResponseChannel.send(response)
                channelFactory.spiderItemChannel.receive()
                logger.debug { "Received the final item" }
                c.stop()
                channelFactory.closeChannels()
            }
        }

    }


    @Test
    fun testBuildCrawlerWithDispatcher() = scope.runTest{
        coroutineScope {
            val c = crawler(this, "MainCrawler", ::testDIModule){
                spiderDispatcher {
                    spider(SpiderClasstest::class, name = "spider1"){
                        urlRequest = "une url 1"
                        state["arg2"] = "value2"
                    }
                    spider(SpiderClasstest::class, name = "spider2"){
                        urlRequest = "une url 2"
                        state["arg2"] = "value2"
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

                channelFactory.spiderItemChannel.receive()
                channelFactory.spiderItemChannel.receive()

                logger.debug { "Received the final item" }
                c.stop()
                channelFactory.closeChannels()
            }
        }

    }


}