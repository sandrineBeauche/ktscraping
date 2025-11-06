package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractSpiderMiddlewareTester: DualScrapingTest() {

    lateinit var middleware: SpiderMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(middlewareName: String): SpiderMiddleware

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        middleware = spyk(buildMiddleware(middlewareName))

        every { middleware.inChannel } returns inChannel

    }

    suspend fun withMiddleware(func: suspend AbstractSpiderMiddlewareTester.() -> Unit){
        coroutineScope {
            every { middleware.scope } returns this
            middleware.start(this)

            func()

            outChannel.close()
            inChannel.close()
            middleware.stop()
        }
    }
}