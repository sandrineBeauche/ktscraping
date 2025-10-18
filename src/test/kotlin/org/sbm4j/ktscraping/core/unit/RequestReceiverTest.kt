package org.sbm4j.ktscraping.core.unit

/*
abstract class RequestReceiverMock(): RequestReceiver {
    override val mutex: Mutex = Mutex()
    override val name: String = "RequestReceiver"

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        (requestIn as Channel).close()
        responseOut.close()
    }
}


class RequestReceiverTest: ScrapingTest<Request, Response<*>>(){

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
            coEvery { receiver.processDataRequest(req) } returns resp

            receiver.performRequests()
            inChannel.send(req)
        }

        coVerify { receiver.answerRequest(req, resp) }
    }

}

 */