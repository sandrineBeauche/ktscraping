package org.sbm4j.meercat.channels

import io.mockk.mockk
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Channelable.Companion.lastId
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.*
import kotlin.test.Test


data class SendA(
    val message: String,
    override var sender: SendSource,
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

    override fun getKeyBarrier(): String {
        return message
    }
}

data class SendC(
    val message: String,
    override var sender: SendSource,
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




class SuperChannelTests {

    @Test
    fun testSendBackExchange() = TestScope().runTest{
        val contA = mockk<SendSource>()

        coroutineScope {
            val channel = SuperChannel.build(this)

            launch(CoroutineName("launchA")){
                repeat(5){
                    val chanA = SendA("messsage #$it from A", sender = contA)
                    val chanB = channel.sendSync<BackB>(chanA)
                    logger.debug{"received ${chanB.loggingLabel} ${chanB.name} for the ${chanA.loggingLabel} ${chanA.name}: ${chanB}"}
                }
                logger.debug{"finished to send sends"}
                channel.close()
            }
            launch(CoroutineName("launchB")){
                channel.getSendFlow().take(5).collect { chanA ->
                    logger.debug{"received ${chanA.name} from ${chanA.sender.name} and answers with a back"}
                    val chanB = chanA.buildBack()
                    channel.send(chanB)
                }
                logger.debug{"finished to answers to sends"}
            }
        }
    }

    @Test
    fun testMultipleSendType() = TestScope().runTest {
        val sender = mockk<SendSource>()

        coroutineScope {
            val channel = SuperChannel.build(this)

            coroutineScope {
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

            channel.close()
        }
    }

    //@Test
    fun testLock() = TestScope().runTest {
        val sender = mockk<SendSource>()
        val s1 = SendA("coucou", sender)
        val s2 = SendA("salut", sender)

        coroutineScope {
            val channel = SuperChannel.build(this)
            coroutineScope {
                launch{
                    channel.send(s1)
                    logger.debug{ "sent ${s1}"}

                    channel.send(s2)
                    logger.debug{ "sent ${s2}"}
                }
                launch{
                    delay(2000L)
                    logger.debug{" Wait for 2 sensds"}
                    channel.getSendFlow().take(2).collect {
                        logger.debug{ "received ${it}"}
                    }

                }
            }
            channel.close()
        }

    }

}