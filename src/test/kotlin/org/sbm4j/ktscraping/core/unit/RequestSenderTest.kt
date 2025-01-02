package org.sbm4j.ktscraping.core.unit

import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.PendingRequestMap
import org.sbm4j.ktscraping.core.RequestException
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.utils.ScrapingTest
import org.sbm4j.ktscraping.requests.AbstractRequest
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

    var closed: Boolean = false

    private fun closeChannels(){
        if(!closed) {
            requestOut.close()
            (responseIn as Channel).close()
            closed = true
        }
    }

    suspend fun callback(response: Response){
        if(response.contents["close"] == true)
            closeChannels()
    }

    suspend fun callbackErr(ex: Throwable){
        val exRequest = ex as RequestException
        if(exRequest.resp.contents["close"] == true)
            closeChannels()
    }

    override suspend fun performResponse(response: Response){
        if(response.contents["close"] == true)
            closeChannels()
    }
}


class RequestSenderTest: ScrapingTest<Response, AbstractRequest>() {


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
        resp.contents["close"] = true

        coroutineScope {
            every { sender.scope } returns this
            sender.receiveResponses()
            sender.send(req, sender::callback, sender::callbackErr)

            val received = outChannel.receive()
            assertEquals(req, received)
            inChannel.send(resp)
        }

        coVerify { sender.callback(resp) }
    }


    @Test
    fun testWithCallbackError() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender, status = Status.NOT_FOUND)
        resp.contents["close"] = true

        coroutineScope {
            every { sender.scope } returns this
            sender.receiveResponses()
            sender.send(req, sender::callback, sender::callbackErr)

            val received = outChannel.receive()
            assertEquals(req, received)
            inChannel.send(resp)
        }

        coVerify { sender.callbackErr(any()) }
    }

    @Test
    fun testWithoutCallback() = TestScope().runTest {
        val (_, resp) = generateRequestResponse(sender)
        resp.contents["close"] = true

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
        resps[0].contents["close"] = false
        resps[1].contents["close"] = true

        every { sender.scope } returns this
        coEvery { sender.callback(resps[0]) } throws Exception("Here is an exception!")

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
        resps[0].contents["close"] = false
        resps[1].contents["close"] = true

        every { sender.scope } returns this
        coEvery { sender.performResponse(resps[0]) } throws Exception("Here is an exception")

        repeat(2){
            inChannel.send(resps[it])
        }
    }
}