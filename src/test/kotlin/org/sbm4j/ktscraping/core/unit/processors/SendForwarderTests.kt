package org.sbm4j.ktscraping.core.unit.processors

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.SendForwarder
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.Test

class TestingSendForwarder(
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel,
    override val name: String = "TestingSendForwarder"
): SendForwarder, AbstractControllable(){

    fun consumeSend(send: Send): Any?{
        logger.debug { "${name}: received a ${send.loggingLabel}: ${send.name}" }
        return send as? StartEvent ?: send.buildBack()
    }

    override suspend fun run() {
        logger.debug { "Running ${name} ..." }
        val clazz = Event::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::consumeSend)
    }

}

class SendForwarderTests {

    val sender = mockk<Controllable>()

    @Test
    fun testSendForwarder1() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingSendForwarder(inChannel, outChannel)

            forwarder.start(this).join()

            val event = StartEvent(sender)
            inChannel.send(event)

            val forwarded = outChannel.getSendFlow().first()

            assertThat(forwarded, sameInstance(event))

            forwarder.stop()

            inChannel.close()
            outChannel.close()
        }
    }

    @Test
    fun testSendForwarder2() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingSendForwarder(inChannel, outChannel)
            val event = EndEvent(sender)

            forwarder.start(this)

            val back = inChannel.sendSync<EventBack>(event)

            assertThat(back.send, sameInstance(event))

            forwarder.stop()
            inChannel.close()
            outChannel.close()
        }
    }
}