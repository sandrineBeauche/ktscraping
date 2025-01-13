package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest

abstract class AbstractMiddlewareTester: DualScrapingTest<AbstractRequest, Response>() {

    val sender: RequestSender = mockk<RequestSender>()

    lateinit var middleware: AbstractMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(sc: CoroutineScope, middlewareName: String): AbstractMiddleware

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
    }

    suspend fun withMiddleware(func: suspend AbstractMiddlewareTester.() -> Unit){
        coroutineScope {
            every { middleware.scope } returns this
            middleware.start()

            func()

            closeChannels()
            middleware.stop()
        }
    }
}