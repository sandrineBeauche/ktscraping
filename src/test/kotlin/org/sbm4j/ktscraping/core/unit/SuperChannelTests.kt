package org.sbm4j.ktscraping.core.unit

import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Channelable.Companion.lastId
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import java.util.*
import kotlin.test.Test


data class SendA(
    val message: String,
    override var sender: Controllable,
    override val name: String = "A${lastId.getAndIncrement()}"
) : Send {

    override var channelableId: UUID = UUID.randomUUID()
    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        return BackB(this,
            Status.ERROR, mutableListOf(infos))
    }

    override fun buildBack(): BackB {
        return BackB(send = this)
    }

    override fun clone(): Send {
        return this.copy()
    }
}

data class SendC(
    val message: String,
    override var sender: Controllable,
    override val name: String = "A${lastId.getAndIncrement()}"
) : Send {

    override var channelableId: UUID = UUID.randomUUID()
    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        return BackD(this,
            Status.ERROR, mutableListOf(infos))
    }

    override fun buildBack(): BackD {
        return BackD(send = this)
    }

    override fun clone(): Send {
        return this.copy()
    }
}

data class BackB(
    override val send: SendA,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "B${lastId.getAndIncrement()}",
    ) : Back<SendA> {

    override var channelableId: UUID = UUID.randomUUID()
    override fun clone(): Back<SendA> {
        return this.copy()
    }
}

data class BackD(
    override val send: SendC,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "D${lastId.getAndIncrement()}",
) : Back<SendC> {

    override var channelableId: UUID = UUID.randomUUID()
    override fun clone(): Back<SendC> {
        return this.copy()
    }
}

class TestingControllableA(
    override val name: String = "ControllableA"
) : AbstractControllable(){

    lateinit var channelableAOut: SuperChannel


    override suspend fun run() {
        scope.launch {
            repeat(5){
                val chanA = SendA("messsage #$it from A", sender = this@TestingControllableA)
                val chanB = channelableAOut.sendSync<BackB>(chanA)
                logger.debug{"${name}: received ${chanB.loggingLabel} ${chanB.name} for the ${chanA.loggingLabel} ${chanA.name}: ${chanB}"}
            }
            logger.debug{"${name}: finished to send sends"}
        }
        scope.launch {
            channelableAOut.getBackFlow(this@TestingControllableA).take(5).collect { chanB ->
                logger.debug{"${name}: received ${chanB.loggingLabel} ${chanB.name} from ${chanB.send.loggingLabel} ${chanB.send.name} (${chanB.send}) -> ${chanB}"}
            }
            logger.debug{"${name}: finished to receive backs"}
        }
    }
}

class TestingControllableB(
    override val name: String = "ControllableB",
) : AbstractControllable(){

    lateinit var channelableAIn: SuperChannel


    override suspend fun run() {
        scope.launch {
            repeat(5){
                val chanA = SendA("another message", sender = this@TestingControllableB)
                val chanB = chanA.buildBack()
                channelableAIn.send(chanB)
                delay(10)
            }
            logger.debug{"${name}: finished to send backs"}
        }
        scope.launch {
            channelableAIn.getSendFlow().take(5).collect { chanA ->
                logger.debug{"${name}: received ${chanA.name} from ${chanA.sender.name} and answers with a back"}
                val chanB = chanA.buildBack()
                channelableAIn.send(chanB)
            }
            logger.debug{"${name}: finished to answers to sends"}
        }
    }
}

class SuperChannelTests {

    @Test
    fun testSendBackExchange() = TestScope().runTest{
        coroutineScope {
            val channel = SuperChannel()

            channel.init()

            val contA = TestingControllableA()
            contA.channelableAOut = channel
            contA.start(this)

            val contB = TestingControllableB()
            contB.channelableAIn = channel
            contB.start(this)

            logger.debug{"Waiting for run to finish"}
            contA.job.join()
            contB.job.join()
            logger.debug { "After join jobs" }

            contA.stop()
            logger.debug{"After stop A"}
            contB.stop()
            logger.debug{"After stop B"}

            channel.close()
            logger.debug{"After channel close()"}
        }
    }

    @Test
    fun testMultipleSendType() = TestScope().runTest {
        val sender = mockk<Controllable>()

        coroutineScope {
            val channel = SuperChannel.build()

            launch {
                repeat(3){
                    val s1 = SendA("coucou$it", sender)
                    channel.send(s1)

                    val s2 = SendC("salut$it", sender)
                    channel.send(s2)
                }
            }
            launch {
                channel.getSendFlow(SendA::class).take(3).collect {
                    logger.debug { "received the sendA: ${it}" }
                }
            }
            launch {
                channel.getSendFlow(SendC::class).take(3).collect {
                    logger.debug { "received the sendC: ${it}" }
                }
            }
        }
    }

    @Test
    fun testLock() = TestScope().runTest {
        val sender = mockk<Controllable>()
        val s1 = SendA("coucou", sender)
        val s2 = SendA("salut", sender)
        val channel = SuperChannel()
        channel.init()

        launch{
            //delay(2000L)
            channel.send(s1)
            logger.debug{ "sent ${s1}"}

            channel.send(s2)
            logger.debug{ "sent ${s2}"}
        }
        launch{
            delay(2000L)
            logger.debug{" Wait for 2 sensds"}
            channel.getSendFlow().collect {
                logger.debug{ "received ${it}"}
            }

        }
    }

    @Test
    fun test2Channels() = TestScope().runTest {
        val channel1 = SuperChannel.build()
        val channel2 = SuperChannel.build()

        val sender = mockk<Controllable>()
        val s1 = SendA("coucou", sender)
        val s2 = SendA("salut", sender)

        coroutineScope {
            launch {
                channel1.send(s1)
            }
            launch {
                val rec1 = channel1.receiveSend<SendA>()
                logger.debug { "received ${rec1}" }
            }
            launch {
                channel2.send(s2)
            }
            launch {
                val rec2 = channel2.receiveSend<SendA>()
                logger.debug { "received ${rec2}" }
            }
        }
    }

}