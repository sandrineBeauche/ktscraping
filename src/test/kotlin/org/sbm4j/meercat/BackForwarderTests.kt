package org.sbm4j.meercat

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.utils.isEventResponseWithError
import org.sbm4j.ktscraping.core.utils.isOKEventBackWith
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import org.sbm4j.meercat.channels.Status
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.BackForwarder
import org.sbm4j.meercat.components.SendSource
import kotlin.test.Test

class TestingBackForwarder(
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel,
    override val name: String = "TestingBackForwarder"
): BackForwarder, AbstractControllable(){

    suspend fun performBack(back: EventBack){
        if(back.send !is StartEvent){
            throw Exception()
        }
    }

    override suspend fun run() {
        val flow = outChannel.getBackFlow(EventBack::class)
        this.receiveBacks(EventBack::class, flow,::performBack)
    }

}

class BackForwarderTests {

    val sender = mockk<SendSource>()

    @Test
    fun testBackForward1() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingBackForwarder(inChannel, outChannel)

            forwarder.start(this).join()

            val event = StartEvent(sender)
            val back = event.buildBack()

            outChannel.send(back)
            val forwarded = inChannel.getBackFlow(EventBack::class).first()

            assertThat(forwarded, isOKEventBackWith("start"))

            forwarder.stop()
            inChannel.close()
            outChannel.close()

        }
    }


    @Test
    fun testBackForward2() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingBackForwarder(inChannel, outChannel)

            forwarder.start(this).join()

            val event = StartEvent(sender)
            val back = event.buildBack()

            val error = ErrorInfo(Exception(), sender, ErrorLevel.MINOR)
            forwarder.pendingMinorError[event.channelableId] = mutableListOf(error)

            outChannel.send(back)
            val forwarded = inChannel.getBackFlow(EventBack::class).first()

            assertThat(forwarded, isEventResponseWithError("start", Status.ERROR, 1))

            forwarder.stop()
            inChannel.close()
            outChannel.close()

        }
    }
}