package org.sbm4j.ktscraping.core.unit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.ktscraping.core.DownloaderRequestDispatcher
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.utils.ScrapingTest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest
import kotlin.test.Test

class DownloaderRequestDispatcherTests: ScrapingTest<Request, Response>() {

    val sender1 : RequestSender = mockk<RequestSender>()

    val dispatcher = spyk(object: DownloaderRequestDispatcher(mockk<CoroutineScope>(), di = mockk<DI>()){
        override fun selectChannel(request: Request): SendChannel<Request> {
            return when(request.url){
                "url1" -> senders[0]
                else -> senders[1]
            }
        }
    })

    val reqChannel1: Channel<Request> = Channel(Channel.UNLIMITED)

    val respChannel1: Channel<Response> = Channel(Channel.UNLIMITED)

    val reqChannel2: Channel<Request> = Channel(Channel.UNLIMITED)

    val respChannel2: Channel<Response> = Channel(Channel.UNLIMITED)

    override fun closeChannels(){
        reqChannel1.close()
        reqChannel2.close()
        respChannel1.close()
        respChannel2.close()
        super.closeChannels()
    }

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        every { dispatcher.channelIn } returns inChannel
        every { dispatcher.channelOut } returns outChannel
        every { dispatcher.senders } returns mutableListOf(reqChannel1, reqChannel2)
        every { dispatcher.receivers } returns mutableListOf(respChannel1, respChannel2)
    }

    @Test
    fun testRequestDispatcherOneRequest() = TestScope().runTest {
        val (req1, resp1) = generateRequestResponse(sender1, "url1")

        coroutineScope {
            launch {
                every{ dispatcher.scope } returns this
                dispatcher.start()
            }
            launch {
                inChannel.send(req1)
                val req = reqChannel1.receive()

                assertThat(req, sameInstance(req1))

                respChannel1.send(resp1)
                val resp = outChannel.receive()

                assertThat(resp, sameInstance(resp1))

                dispatcher.stop()
                closeChannels()
            }
        }
    }
}