package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractMiddlewareTester: DualScrapingTest() {

    val sender: Controllable = mockk<Controllable>()

    lateinit var middleware: AbstractMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(middlewareName: String): AbstractMiddleware

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        middleware = spyk(buildMiddleware(middlewareName))

        every { middleware.inChannel } returns inChannel
        every { middleware.outChannel } returns outChannel
    }


    suspend fun performEvent(event: Event, eventBack: EventBack){
        inChannel.send(event)
        outChannel.channel.receive() as Event
        logger.info{"received forwarded ${event.eventName} event"}

        logger.info{ "send back for ${event.eventName} event"}
        outChannel.send(eventBack)
        inChannel.channel.receive()
    }

    suspend fun performStartEvent(){
        val startEvent = StartEvent(sender)
        val startResponse = startEvent.buildBack()

        performEvent(startEvent, startResponse)
    }

    suspend fun performEndEvent(){
        val endEvent = EndEvent(sender)
        val endEventBack = endEvent.buildBack()

        performEvent(endEvent, endEventBack)
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