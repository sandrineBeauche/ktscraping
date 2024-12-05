package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest

abstract class AbstractSpiderMiddlewareTester: DualScrapingTest<AbstractRequest, Response>() {

    val sender: RequestSender = mockk<RequestSender>()

    lateinit var middleware: SpiderMiddleware

    val middlewareName: String = "Middleware"

    val itemChannelIn: Channel<Item> = Channel<Item>(Channel.UNLIMITED)

    val itemChannelOut: Channel<Item> = Channel<Item>(Channel.UNLIMITED)

    abstract fun buildMiddleware(sc: CoroutineScope, middlewareName: String): SpiderMiddleware

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        middleware = spyk(buildMiddleware(sc, middlewareName))

        every { middleware.requestIn } returns inChannel
        every { middleware.requestOut } returns followInChannel
        every { middleware.responseIn } returns outChannel
        every { middleware.responseOut } returns followOutChannel
        every { middleware.itemIn } returns itemChannelIn
        every { middleware.itemOut } returns itemChannelOut
    }

    suspend fun withMiddleware(func: suspend AbstractSpiderMiddlewareTester.() -> Unit){
        coroutineScope {
            every { middleware.scope } returns this
            middleware.start()

            func()

            outChannel.close()
            inChannel.
            close()
            itemChannelIn.close()
            itemChannelOut.close()
            middleware.stop()
        }
    }
}