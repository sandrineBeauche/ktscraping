package org.sbm4j.meercat

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.sbm4j.meercat.channels.ChannelManager
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.StringBack
import org.sbm4j.meercat.data.StringSend
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.dispatchers.AbstractRouter
import org.sbm4j.meercat.nodes.sendProcessors.AbstractInitiator
import kotlin.test.Test

class StringSink(
    name: String,
    override var inChannel: SuperChannel
) : AbstractSinkNode(name) {

    val receivedSends: MutableList<StringSend> = mutableListOf()

    suspend fun processSend(send: StringSend): Any {
        receivedSends.add(send)
        return send.buildBack()
    }

    override suspend fun run() {
        val clazz = StringSend::class
        val flow = inChannel.getSendFlow(clazz)
        performSends(clazz, flow, ::processSend)

        super.run()
    }
}

class StringForwarder(
    name: String,
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel
) : AbstractMiddleNode(name) {

    suspend fun processSend(send: StringSend): Any {
        return send
    }

    suspend fun processBack(back: StringBack) {
    }

    override suspend fun run() {
        val clazz = StringSend::class
        val flow = inChannel.getSendFlow(clazz)
        performSends(clazz, flow, ::processSend)

        val backClazz = StringBack::class
        val backFlow = outChannel.getBackFlow(backClazz)
        this.receiveBacks(backClazz, backFlow, ::processBack)

        super.run()
    }
}


class StringInitiator(
    override val name: String,
    override var outChannel: SuperChannel,
    val sends: List<String>
) : AbstractInitiator() {

    val backs = mutableListOf<StringBack>()

    override suspend fun run() {
        sends.forEach { value ->
            val send = StringSend(value, this)
            val back = outChannel.sendSync<StringBack>(send)
            backs.add(back)
        }
    }
}

class StringRouter(
    override val name: String,
    override var channelIn: SuperChannel,
    val channel1: SuperChannel,
    val channel2: SuperChannel
) : AbstractRouter() {

    init {
        channelOuts.addAll(listOf(channel1, channel2))
    }

    /*
    override suspend fun selectFuncChannel(send: Send): SuperChannel {
        send as StringSend
        return if (send.value == "bonjour") channel1 else channel2
    }

     */

    override suspend fun run() {
        val clazz = StringSend::class
        val flow = channelIn.getSendFlow(clazz)

        //val predicate: suspend (Send) -> Boolean = { send -> send is StringSend }
        //performSendBacks()
        //performSends(clazz, flow, ::selectFuncChannel)

        super.run()
    }
}

class SimpleTopologyTests {

    private lateinit var parentScope: CoroutineScope

    @BeforeEach
    fun setup() {
        parentScope = CoroutineScope(Dispatchers.Default)
    }

    @AfterEach
    fun tearDown() {
        parentScope.cancel()
    }

    @Test
    fun `initiator forwarder sink`() = runBlocking {
        val channel1 = SuperChannel("channel1")
        val channel2 = SuperChannel("channel2")

        val initiator = StringInitiator("initiator", channel1, listOf("item1", "item2", "item3"))
        val forwarder = spyk(StringForwarder("forwarder", channel1, channel2))
        val sink = spyk(StringSink("sink", channel2))

        val channelManager = ChannelManager()
        channelManager.channels.addAll(listOf(channel1, channel2))
        val topologyManager = TopologyManager(channelManager).apply {
            nodes.addAll(listOf(initiator, forwarder, sink))
        }

        topologyManager.start(parentScope)?.join()
        topologyManager.waitCompleted()

        coVerify(exactly = 3) { forwarder.processSend(any()) }
        assertThat(sink.receivedSends, hasSize(equalTo(3)))
        assertThat(initiator.backs, hasSize(equalTo(3)))

        topologyManager.stop()
    }


}