package org.sbm4j.ktscraping.core.unit

import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.RequestReceiver
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.utils.ScrapingTest
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest
import kotlin.test.Test

abstract class RequestReceiverMock(): RequestReceiver {
    override val mutex: Mutex = Mutex()
    override val name: String = "RequestReceiver"

    override suspend fun answerRequest(request: AbstractRequest, result: Any?) {
        (requestIn as Channel).close()
        responseOut.close()
    }
}


class RequestReceiverTest: ScrapingTest<Request, Response>(){

    val sender : RequestSender = mockk<RequestSender>()
    val receiver: RequestReceiverMock = spyk<RequestReceiverMock>()

    @BeforeTest
    fun setUp() {
        initChannels()
        clearAllMocks()

        every { receiver.requestIn } returns inChannel
        every { receiver.responseOut } returns outChannel
    }

    @Test
    fun testWithRequest() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender)
        

        coroutineScope {
            every { receiver.scope } returns this
            every { receiver.processRequest(req) } returns resp

            receiver.performRequests()
            inChannel.send(req)
        }

        coVerify { receiver.answerRequest(req, resp) }
    }

}