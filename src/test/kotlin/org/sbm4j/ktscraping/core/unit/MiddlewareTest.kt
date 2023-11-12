package org.sbm4j.ktscraping.core.unit

import io.mockk.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.Middleware
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.utils.DualScrapingTest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class MiddlewareMock(): Middleware {
    override val mutex: Mutex = Mutex()
    override val name: String = "Middleware"
    override fun processRequest(request: Request): Any? {
        request.url = "Another url"
        return request
    }

    override fun processResponse(response: Response): Boolean {
        return true
    }
}

class MiddlewareTest: DualScrapingTest<Request, Response>() {

    val sender: RequestSender = mockk<RequestSender>()

    lateinit var middleware: Middleware

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        middleware = spyk<MiddlewareMock>()
        every { middleware.requestIn } returns inChannel
        every { middleware.requestOut } returns followInChannel
        every { middleware.responseIn } returns outChannel
        every { middleware.responseOut } returns followOutChannel
    }

    @Test
    fun testFollowingRequestAndResponse() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender)

        coroutineScope {
            every { middleware.scope } returns this
            middleware.start()

            inChannel.send(req)
            val receivedReq = followInChannel.receive()

            assertEquals(req.name, receivedReq.name)
            assertEquals("Another url", receivedReq.url)

            outChannel.send(resp)
            val receivedResp = followOutChannel.receive()

            closeChannels()
        }

        verify { middleware.processRequest(req)}
        verify { middleware.processResponse(resp)}
    }
}