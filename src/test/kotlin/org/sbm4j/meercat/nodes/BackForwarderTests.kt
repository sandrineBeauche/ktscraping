package org.sbm4j.meercat.nodes

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.data.TestingBack
import org.sbm4j.meercat.data.TestingSend
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import org.sbm4j.meercat.testingBack
import org.sbm4j.meercat.testingBackWithErrors
import kotlin.test.Test

class TestingBackForwarder(
    override var inChannel: SuperChannel,
    override var outChannel: SuperChannel,
    override val name: String = "TestingBackForwarder"
): SendForwarder, BackForwarder, AbstractComponent(){

    suspend fun performBack(back: TestingBack){
        if(back.send.value != "coucou"){
            throw Exception()
        }
    }

    override suspend fun run() {
        val flow = outChannel.getBackFlow(TestingBack::class)
        this.receiveBacks(TestingBack::class, flow,::performBack)
    }

}

class BackForwarderTests: MiddleNodeTester<TestingBackForwarder>() {

    override fun buildNode(): TestingBackForwarder {
        return TestingBackForwarder(inChannel, outChannel)
    }

    @Test
    fun `simple back forward`() = TestScope().runTest {
        val send = TestingSend("coucou", sender)
        outChannel.send(send)

        val forwarded = inChannel.getBackFlow(TestingBack::class).first()

        assertThat(forwarded, testingBack("coucou"))
    }

    @Test
    fun testBackForward2() = TestScope().runTest {
            val send = TestingSend("coucou", sender)

            val error = ErrorInfo(Exception(), sender, ErrorLevel.MINOR)
            node.pendingMinorError[send.channelableId] = mutableListOf(error)

            outChannel.send(send)
            val forwarded = inChannel.getBackFlow(TestingBack::class).first()

            assertThat(forwarded,
                testingBackWithErrors(
                    "coucou", Status.ERROR, 1))

    }


}