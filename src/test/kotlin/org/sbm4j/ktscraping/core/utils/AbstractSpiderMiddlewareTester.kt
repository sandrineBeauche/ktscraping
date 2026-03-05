package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import kotlin.test.BeforeTest

abstract class AbstractSpiderMiddlewareTester: DualScrapingTest() {

    lateinit var middleware: SpiderMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(middlewareName: String): SpiderMiddleware

    @BeforeTest
    fun setUp(){
        buildChannels()
        clearAllMocks()

        middleware = buildMiddleware(middlewareName)

        middleware.inChannel = inChannel

    }

    suspend fun withMiddleware(func: suspend AbstractSpiderMiddlewareTester.() -> Unit){
        coroutineScope {
            initChannels(this)
            middleware.start(this)

            func()

            outChannel.close()
            inChannel.close()
            middleware.stop()
        }
    }
}