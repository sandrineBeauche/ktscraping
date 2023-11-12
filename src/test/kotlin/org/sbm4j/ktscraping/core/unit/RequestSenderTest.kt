package org.sbm4j.ktscraping.core.unit

import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.PendingRequestMap
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.utils.ScrapingTest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class RequestSenderMock() : RequestSender {
    override val mutex = Mutex()
    override val name = "RequestSender"
    override var pendingRequests = PendingRequestMap()

    private fun closeChannels(){
        requestOut.close()
        (responseIn as Channel).close()
    }

    suspend fun callback(request: Request, response: Response){
        closeChannels()
    }

    suspend fun callbackErr(request: Request, response: Response){
        closeChannels()
    }

    override suspend fun performResponse(response: Response){
        closeChannels()
    }
}


class RequestSenderTest: ScrapingTest<Response, Request>() {


    val sender: RequestSenderMock = spyk<RequestSenderMock>()


    @BeforeTest
    fun setUp() {
        initChannels()

        clearAllMocks()
        every { sender.requestOut } returns outChannel
        every { sender.responseIn } returns inChannel
    }


    @Test
    fun testWithCallback() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender)

        coroutineScope {
            every { sender.scope } returns this
            sender.receiveResponses()
            sender.send(req, sender::callback, sender::callbackErr)

            val received = outChannel.receive()
            assertEquals(req, received)
            inChannel.send(resp)
        }

        coVerify { sender.callback(req, resp) }
    }


    @Test
    fun testWithCallbackError() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender, status = Status.NOT_FOUND)

        coroutineScope {
            every { sender.scope } returns this
            sender.receiveResponses()
            sender.send(req, sender::callback, sender::callbackErr)

            val received = outChannel.receive()
            assertEquals(req, received)
            inChannel.send(resp)
        }

        coVerify { sender.callbackErr(req, resp) }
    }

    @Test
    fun testWithoutCallback() = TestScope().runTest {
        val (_, resp) = generateRequestResponse(sender)

        coroutineScope {
            every { sender.scope } returns this
            sender.receiveResponses()

            inChannel.send(resp)
        }

        coVerify { sender.performResponse(resp) }
    }

    @Test
    fun testWithCallbackException() = TestScope().runTest {
        val (reqs, resps) = generateRequestResponses(sender)

        every { sender.scope } returns this
        coEvery { sender.callback(reqs[0], resps[0]) } throws Exception("Here is an exception!")

        sender.receiveResponses()

        repeat(2) {
            sender.send(reqs[it], sender::callback, sender::callbackErr)

            val received = outChannel.receive()
            assertEquals(reqs[it], received)
            inChannel.send(resps[it])
        }
    }

    @Test
    fun testWithoutCallbackException() = TestScope().runTest {
        val (_, resps) = generateRequestResponses(sender)

        every { sender.scope } returns this
        coEvery { sender.performResponse(resps[0]) } throws Exception("Here is an exception")

        repeat(2){
            inChannel.send(resps[it])
        }
    }
}