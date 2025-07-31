package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest<Response<*>, AbstractRequest>(){

    lateinit var spider: AbstractSpider

    val itemChannel: Channel<Item> = Channel<Item>(Channel.UNLIMITED)

    val spiderName: String = "Spider"

    abstract fun buildSpider(spiderName: String): AbstractSpider

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        spider = spyk(buildSpider(spiderName))

        every { spider.requestOut } returns outChannel
        every { spider.responseIn } returns inChannel
        every { spider.itemsOut } returns itemChannel
    }

    suspend fun withSpider(func: suspend AbstractSpiderTester.() -> Unit){
        coroutineScope {
            every { spider.scope } returns this
            spider.start(this)

            val startReq = outChannel.receive() as EventRequest
            val startResp = EventResponse(startReq.eventName, startReq)
            inChannel.send(startResp)

            func()

            val endReq = outChannel.receive() as EventRequest
            val endResp = EventResponse(endReq.eventName, endReq)
            inChannel.send(endResp)

            outChannel.close()
            inChannel.close()
            itemChannel.close()
            spider.stop()
        }
    }
}