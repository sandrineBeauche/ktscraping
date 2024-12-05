package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest<Response, AbstractRequest>(){

    lateinit var spider: AbstractSpider

    val itemChannel: Channel<Item> = Channel<Item>(Channel.UNLIMITED)

    val spiderName: String = "Spider"

    abstract fun buildSpider(sc: CoroutineScope, spiderName: String): AbstractSpider

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        spider = spyk(buildSpider(sc, spiderName))

        every { spider.requestOut } returns outChannel
        every { spider.responseIn } returns inChannel
        every { spider.itemsOut } returns itemChannel
    }

    suspend fun withSpider(func: suspend AbstractSpiderTester.() -> Unit){
        coroutineScope {
            every { spider.scope } returns this
            spider.start()

            func()

            outChannel.close()
            inChannel.close()
            itemChannel.close()
            spider.stop()
        }
    }
}