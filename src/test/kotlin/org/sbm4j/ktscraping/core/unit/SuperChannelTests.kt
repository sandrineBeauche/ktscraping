package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.State
import org.sbm4j.ktscraping.core.SuperChannel
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.Response
import java.util.UUID
import kotlin.test.Test


class ChannelableA(
    val message: String,
    override var sender: Controllable?
) : Channelable{
    override var channelableId: UUID = UUID.randomUUID()
}

class ChannelableB(
    val message: String,
    override var sender: Controllable?
) : Channelable{
    override var channelableId: UUID = UUID.randomUUID()
}

class TestingControllableA(
    override val name: String = "ControllableA"
) : Controllable{
    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var scope: CoroutineScope

    lateinit var channelableAOut: SendChannel<ChannelableA>

    lateinit var channelableBIn: Flow<ChannelableB>

    override suspend fun run() {
        scope.launch {
            repeat(5){
                val chanA = ChannelableA("messsage #$it from A", this@TestingControllableA)
                channelableAOut.send(chanA)
            }
        }
        scope.launch {
            channelableBIn.collect {
                println("${name}: received ${it.message} from ${it.sender?.name}")
            }
        }
    }
}

class TestingControllableB(
    override val name: String = "ControllableB",
) : Controllable{
    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var scope: CoroutineScope

    lateinit var channelableAIn: Flow<ChannelableA>

    lateinit var channelableBOut: SendChannel<ChannelableB>

    override suspend fun run() {
        scope.launch {
            repeat(5){
                val chanA = ChannelableB("messsage #$it from B", this@TestingControllableB)
                channelableBOut.send(chanA)
            }
        }
        scope.launch {
            channelableAIn.collect {
                println("${name}: received ${it.message} from ${it.sender?.name}")
            }
        }
    }
}

class SuperChannelTests {

    @Test
    fun testRequestResponseExchange() = TestScope().runTest{
        coroutineScope {
            val channel = SuperChannel(this)

            launch {
                val contA = TestingControllableA()
                contA.channelableAOut = channel.channel
                contA.channelableBIn = channel.getFlow<ChannelableB>()
                contA.start(this)

                val contB = TestingControllableB()
                contB.channelableAIn = channel.getFlow<ChannelableA>()
                contB.channelableBOut = channel.channel
                contB.start(this)


            }
        }
    }
}