package org.sbm4j.ktscraping.core.unit.components

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test

class TestingEventConsumer(
    override var inChannel: SuperChannel,
    override val name: String = "TestingEventConsumer"
): EventConsumer, AbstractComponent(){

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
            = ConcurrentHashMap()

    override suspend fun sendPostProcess(send: Send, result: Any) {
        logger.debug{"${name}: processed ${send.loggingLabel}: ${send}"}
        val back = send.buildBack()
        inChannel.send(back)
    }

    override suspend fun preStart(event: Event): EventJobResult? {
        logger.trace{"${name}: inside pre Start event"}
        return super.preStart(event)
    }
}

class EventConsumerTests {

    val sender = mockk<SendSource>()

    @Test
    fun testEventConsume1() = TestScope().runTest {
        coroutineScope {
            val channel = SuperChannel.build(this)

            val consumer = TestingEventConsumer(channel)

            consumer.start(this)?.join()

            val event = StartEvent(sender)
            val back = channel.sendSync<EventBack>(event)
            logger.debug { "received back from start event: ${back}" }

            consumer.stop()
            channel.close()
        }
    }
}