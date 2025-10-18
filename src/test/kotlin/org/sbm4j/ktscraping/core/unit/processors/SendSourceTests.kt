package org.sbm4j.ktscraping.core.unit.processors

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.SendException
import org.sbm4j.ktscraping.core.processors.SendSource
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import kotlin.test.Test

class TestingSendSource(
    override var outChannel: SuperChannel,
    override val name: String = "TestingSendSource"
): SendSource, AbstractControllable(){

    fun okBack(back: Back<*>){
        logger.debug {"${name}: received a back with OK status: ${back}"}
    }

    fun errorBack(ex: SendException){
        logger.debug{"${name}: received a back with errors: ${ex.resp}"}
    }

    override suspend fun run() {
        val event = StartEvent(this)
        try {
            val back = this.sendSync(event)
            okBack(back)
        }
        catch(ex: SendException){
            errorBack(ex)
        }
    }


}

class SendSourceTests {

    val anotherControllable = mockk<AbstractControllable>()

    @Test
    fun testSendSource() = TestScope().runTest {
        val channel = SuperChannel.build()

        val component = spyk(TestingSendSource(channel))

        coroutineScope {
            launch {
                component.start(this)
            }
            launch {
                val send = channel.getSendFlow().first()
                val back = send.buildBack()
                channel.send(back)
            }
        }

        verify { component.okBack(any())}
    }


    @Test
    fun testSendSourceErrors() = TestScope().runTest {
        val channel = SuperChannel()
        channel.init()

        val component = spyk(TestingSendSource(channel))

        coroutineScope {
            launch {
                component.start(this)
            }
            launch {
                val send = channel.getSendFlow().first()
                val errorsInfos = ErrorInfo(Exception(), anotherControllable, ErrorLevel.MAJOR)
                val back = send.buildErrorBack(errorsInfos)
                channel.send(back)
            }
        }

        verify { component.errorBack(any())}
    }


}