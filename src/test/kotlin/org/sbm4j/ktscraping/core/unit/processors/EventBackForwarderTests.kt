package org.sbm4j.ktscraping.core.unit.processors

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
import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.Test

class TestingEventBackForwarder(
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel,
    override val name: String = "TestingEventBackForwarder"
) : EventBackForwarder, AbstractControllable() {

    override suspend fun postStart(event: EventBack) {
        logger.debug{"${name}: inside post start"}
    }

    override suspend fun run() {
        super<EventBackForwarder>.run()
    }

}

class EventBackForwarderTests {

    val sender = mockk<Controllable>()

    @Test
    fun testEventBack() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingEventBackForwarder(inChannel, outChannel)

            forwarder.start(this).join()

            val event = StartEvent(sender)
            val back = event.buildBack()
            outChannel.send(back)

            val receivedBack = inChannel.getBackFlow().first()

            logger.debug { "received back from start event: ${receivedBack}" }

            forwarder.stop()
            inChannel.close()
            outChannel.close()
        }
    }
}