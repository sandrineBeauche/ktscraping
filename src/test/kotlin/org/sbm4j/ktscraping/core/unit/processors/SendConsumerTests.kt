package org.sbm4j.ktscraping.core.unit.processors

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.SendConsumer
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test

class TestingSendConcumer(
    override var inChannel: SuperChannel,
    override val name: String = "TestingConsumer"
): SendConsumer, AbstractControllable() {
    override suspend fun sendPostProcess(send: Send, result: Any) {
        logger.debug{"${name}: processed ${send.loggingLabel} ${send.name}, sending back"}
        inChannel.send(result as Channelable)
    }

    suspend fun consumeSend(send: Event): Any?{
        logger.debug{"${name}: received a ${send.loggingLabel}: ${send.name}"}
        return send.buildBack()
    }

    override suspend fun run() {
        logger.debug { "Running ${name} ..." }
        val clazz = Event::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::consumeSend)
    }
}

class TestingSendConcumer2(
    override var inChannel: SuperChannel,
    override val name: String = "TestingConsumer2"
): SendConsumer, AbstractControllable() {
    override suspend fun sendPostProcess(send: Send, result: Any) {
        logger.debug{"${name}: processed ${send.loggingLabel} ${send.name}, sending back"}
        inChannel.send(result as Channelable)
    }

    suspend fun consumeEvent(send: Event): Any?{
        logger.debug{"${name}: inside consumeEvent -> received a ${send.loggingLabel}: ${send.name}"}
        return send.buildBack()
    }

    suspend fun consumeRequest(send: AbstractRequest): Any?{
        logger.debug{"${name}: inside consumeRequest -> received a ${send.loggingLabel}: ${send.name}"}
        return send.buildBack()
    }

    override suspend fun run() {
        logger.debug { "Running ${name} ..." }
        val clazz1 = Event::class
        val flow1 = inChannel.getSendFlow(clazz1)
        this.performSends(clazz1, flow1, ::consumeEvent)

        val clazz2 = AbstractRequest::class
        val flow2 = inChannel.getSendFlow(clazz2)
        this.performSends(clazz2, flow2, ::consumeRequest)
    }
}





class SendConsumerTests {

    val sender = mockk<Controllable>()

    @Test
    fun testSendConsumer() = TestScope().runTest {
        coroutineScope {
            val channel = SuperChannel.build(this)

            val consumer = TestingSendConcumer(channel)
            consumer.start(this).join()

            val event = StartEvent(sender)
            val back = channel.sendSync<EventBack>(event)
            logger.debug { "received back from start event: ${back}" }

            consumer.stop()
            channel.close()
        }
    }

    @Test
    fun testMultipleSendTypeConsumer() = TestScope().runTest {
        coroutineScope {
            val channel = SuperChannel.build(this)

            val consumer = TestingSendConcumer2(channel)
            consumer.start(this).join()

            coroutineScope {
                repeat(3) {
                    launch {
                        val event = StartEvent(sender)
                        val back = channel.sendSync<EventBack>(event)
                        logger.debug { "received back from start event: ${back}" }
                    }
                    launch{
                        val request = Request(sender, "an url")
                        val back2 = channel.sendSync<DownloadingResponse>(request)
                        logger.debug { "received back from request: ${back2}" }
                    }
                }
            }

            consumer.stop()
            channel.close()
        }
    }
}