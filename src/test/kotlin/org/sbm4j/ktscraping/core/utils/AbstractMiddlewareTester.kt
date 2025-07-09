package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractMiddlewareTester: DualScrapingTest<AbstractRequest, Response<*>>() {

    val sender: RequestSender = mockk<RequestSender>()

    lateinit var middleware: AbstractMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(middlewareName: String): AbstractMiddleware

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        middleware = spyk(buildMiddleware(middlewareName))

        every { middleware.requestIn } returns inChannel
        every { middleware.requestOut } returns forwardInChannel
        every { middleware.responseIn } returns outChannel
        every { middleware.responseOut } returns forwardOutChannel
    }

    suspend fun withMiddleware(func: suspend AbstractMiddlewareTester.() -> Unit){
        coroutineScope {
            middleware.start(this)

            func()

            closeChannels()
            middleware.stop()
        }
    }
}