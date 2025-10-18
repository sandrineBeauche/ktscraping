package org.sbm4j.ktscraping.core.unit

import org.sbm4j.ktscraping.core.dispatchers.BackDispatcher

abstract class ResponseDispatcherMock: BackDispatcher{
    //override val pendingAnswerable: PendingRequestMap = PendingRequestMap()
    override val name: String = "ResponseDispatcherMock"
}
/*
class ResponseDispatcherTest: ScrapingTest<Response<*>, AbstractRequest>(){

    val sender1 : RequestSender = mockk<RequestSender>()

    var dispatcher: ResponseDispatcherMock = spyk<ResponseDispatcherMock>()

    val reqChannel1: Channel<Request> = Channel(Channel.UNLIMITED)

    val respChannel1: Channel<Response<*>> = Channel(Channel.UNLIMITED)

    val reqChannel2: Channel<Request> = Channel(Channel.UNLIMITED)

    val respChannel2: Channel<Response<*>> = Channel(Channel.UNLIMITED)

    val senders : MutableMap<ReceiveChannel<AbstractRequest>, SendChannel<Response<*>>> = mutableMapOf(
        reqChannel1 to respChannel1,
        reqChannel2 to respChannel2
    )

    override fun closeChannels(){
        reqChannel1.close()
        reqChannel2.close()
        super.closeChannels()
    }

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        every { dispatcher.channelIn } returns inChannel
        every { dispatcher.channelOut } returns outChannel
    }

    @Test
    fun testResponseDispatcherOneRequest() = TestScope().runTest{

        val (req1, resp1) = generateRequestResponse(sender1)

        coroutineScope {
            launch{
                every { dispatcher.scope } returns this
                every { dispatcher.senders } returns this@ResponseDispatcherTest.senders

                dispatcher.start(this)
            }
            launch{
                reqChannel1.send(req1)
                val req = outChannel.receive()

                assertThat(req, sameInstance(req1))

                inChannel.send(resp1)
                val resp = respChannel1.receive()
                assertThat(resp, sameInstance(resp1))

                dispatcher.stop()
                closeChannels()
            }
        }
    }

    @Test
    fun testResponseDispatcherTwoRequest() = TestScope().runTest{

        val (req1, resp1) = generateRequestResponse(sender1)
        val (req2, resp2) = generateRequestResponse(sender1)

        coroutineScope {
            launch{
                every { dispatcher.scope } returns this
                every { dispatcher.senders } returns this@ResponseDispatcherTest.senders

                dispatcher.start(this)
            }
            launch{
                reqChannel1.send(req1)
                reqChannel2.send(req2)

                outChannel.receive()
                outChannel.receive()

                inChannel.send(resp1)
                inChannel.send(resp2)

                val result1 = respChannel1.receive()
                val result2 = respChannel2.receive()

                assertThat(result1, sameInstance(resp1))
                assertThat(result2, sameInstance(resp2))

                dispatcher.stop()
                closeChannels()
            }
        }
    }
}

 */