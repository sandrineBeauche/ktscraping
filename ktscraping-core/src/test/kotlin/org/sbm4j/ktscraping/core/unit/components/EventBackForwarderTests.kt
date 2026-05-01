package org.sbm4j.ktscraping.core.unit.components

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test

class TestingEventBackForwarder(
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel,
    override val name: String = "TestingEventBackForwarder",
) : EventBackForwarder, AbstractComponent() {

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
        = ConcurrentHashMap()

    override suspend fun postStart(event: EventBack) {
        logger.debug{"${name}: inside post start"}
    }

    override suspend fun run() {
        super<EventBackForwarder>.run()
    }

}

class EventBackForwarderTests {

    val sender = mockk<SendSource>()

    @Test
    fun testEventBack() = TestScope().runTest {
        coroutineScope {
            val inChannel = SuperChannel.build(this)
            val outChannel = SuperChannel.build(this)

            val forwarder = TestingEventBackForwarder(inChannel, outChannel)

            forwarder.start(this)?.join()

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