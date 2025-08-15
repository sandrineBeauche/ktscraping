package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.SuperChannel
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest(){

    lateinit var spider: AbstractSpider

    lateinit var channel: SuperChannel

    val spiderName: String = "Spider"

    abstract fun buildSpider(spiderName: String): AbstractSpider

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        spider = spyk(buildSpider(spiderName))

        channel = SuperChannel()
        every { spider.outChannel } returns channel
    }

    suspend fun withSpider(func: suspend AbstractSpiderTester.() -> Unit){
        coroutineScope {
            every { spider.scope } returns this
            channel.scope = spider.scope

            spider.start(this)

            val startReq = channel.channel.receive() as EventRequest
            val startResp = EventResponse(startReq)
            inChannel.send(startResp)

            func()

            val endReq = channel.channel.receive() as EventRequest
            val endResp = EventResponse(endReq)
            inChannel.send(endResp)

            channel.close()
            spider.stop()
        }
    }
}