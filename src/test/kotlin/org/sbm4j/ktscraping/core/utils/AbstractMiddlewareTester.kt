package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

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


    suspend fun performEvent(eventRequest: EventRequest, eventResponse: EventResponse){
        inChannel.send(eventRequest)
        forwardInChannel.receive() as EventRequest
        logger.info{"received forwarded ${eventRequest.eventName} event"}

        logger.info{ "send response for ${eventRequest.eventName} event"}
        outChannel.send(eventResponse)
        forwardOutChannel.receive()
    }

    suspend fun performStartEvent(){
        val startRequest = StartRequest(sender)
        val startResponse = EventResponse(startRequest.eventName, startRequest)

        performEvent(startRequest, startResponse)
    }

    suspend fun performEndEvent(){
        val endRequest = EndRequest(sender)
        val endResponse = EventResponse(endRequest.eventName, endRequest)

        performEvent(endRequest, endResponse)
    }



    suspend fun withMiddleware(func: suspend AbstractMiddlewareTester.() -> Unit){
        coroutineScope {
            middleware.start(this)
            performStartEvent()

            func()

            performEndEvent()
            closeChannels()
            middleware.stop()
        }
    }
}