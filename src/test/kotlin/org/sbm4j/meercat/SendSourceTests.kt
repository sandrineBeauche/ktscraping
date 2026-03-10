package org.sbm4j.meercat

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.processors.SendException
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.SendSource
import org.sbm4j.meercat.components.logger
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSendSource() = TestScope().runTest {
        coroutineScope {
            DebugProbes.install() // si pas déjà fait

            val channel = SuperChannel.build(this)

            val component = spyk(TestingSendSource(channel))

            coroutineScope {
                launch {
                    component.start(this).join()
                    logger.debug{"Component finished. Stop it"}
                }
                launch {
                    val send = channel.getSendFlow().first()
                    val back = send.buildBack()
                    channel.send(back)

                    //DebugProbes.dumpCoroutines()
                    //component.job.join()
                    component.stop()
                }
            }

            channel.close()
            verify { component.okBack(any()) }
        }
    }


    @Test
    fun testSendSourceErrors() = TestScope().runTest {
        coroutineScope {
            val channel = SuperChannel.build(this)

            val component = spyk(TestingSendSource(channel))

            coroutineScope {
                launch {
                    component.start(this).join()
                    logger.debug { "Component finished. Stop it" }
                    component.stop()
                }
                launch {
                    val send = channel.getSendFlow().first()
                    val errorsInfos = ErrorInfo(Exception(), anotherControllable, ErrorLevel.MAJOR)
                    val back = send.buildErrorBack(errorsInfos)
                    channel.send(back)
                }
            }

            channel.close()
            verify { component.errorBack(any()) }
        }
    }

}