package org.sbm4j.meercat.nodes

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.*
import org.sbm4j.meercat.nodes.sendProcessors.Initiator
import org.sbm4j.meercat.nodes.sendProcessors.NodeStatus
import kotlin.test.Test

class TestingSendSource(
    override var outChannel: SuperChannel,
    override val name: String = "TestingSendSource"
): Initiator, AbstractProcessingNode(){

    override lateinit var initiatorStatus: MutableStateFlow<NodeStatus>

    fun okBack(back: Back<*>){
        logger.debug {"${name}: received a back with OK status: ${back}"}
    }

    fun errorBack(ex: SendException){
        logger.debug{"${name}: received a back with errors: ${ex.resp}"}
    }

    override suspend fun run() {
        val event = TestingSend("coucou", this)
        try {
            val back = this.sendSync(event)
            okBack(back)
        }
        catch(ex: SendException){
            errorBack(ex)
        }
    }

}

class SendSourceTests: InitiatorTester<TestingSendSource>() {

    override fun buildNode(): TestingSendSource {
        return spyk(TestingSendSource(outChannel))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSendSource() = TestScope().runTest {
        startAndWait()
        verify { node.okBack(any()) }
    }


    @Test
    fun testSendSourceErrors() = TestScope().runTest {
        stub.respondWithError(message = "error")
        startAndWait()
        verify { node.errorBack(any()) }
    }
}