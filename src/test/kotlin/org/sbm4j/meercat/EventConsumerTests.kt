package org.sbm4j.meercat

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.channels.Send
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.EventConsumer
import org.sbm4j.meercat.components.EventJobResult
import org.sbm4j.meercat.components.SendSource
import org.sbm4j.meercat.components.logger
import kotlin.test.Test

class TestingEventConsumer(
    override var inChannel: SuperChannel,
    override val name: String = "TestingEventConsumer"
): EventConsumer, AbstractControllable(){

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

            consumer.start(this).join()

            val event = StartEvent(sender)
            val back = channel.sendSync<EventBack>(event)
            logger.debug { "received back from start event: ${back}" }

            consumer.stop()
            channel.close()
        }
    }
}