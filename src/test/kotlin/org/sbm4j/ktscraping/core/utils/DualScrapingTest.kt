package org.sbm4j.ktscraping.core.utils

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.meercat.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel

abstract class DualScrapingTest: ScrapingTest() {

    lateinit var outChannel: SuperChannel

    val errorSender = mockk<Component>()


    override fun buildChannels() {
        super.buildChannels()
        outChannel = SuperChannel()
    }

    override fun initChannels(parentScope: CoroutineScope) {
        super.initChannels(parentScope)
        outChannel.init(parentScope)
    }

    override fun closeChannels(){
        super.closeChannels()
        outChannel.close()
    }

    suspend fun processSend(send: Send){
        val back = if(send.sender == sender){
            send.buildBack()
        }
        else{
            val error = ErrorInfo(
                Exception("Exception for the ${send.loggingLabel} ${send}"),
                errorSender,
                ErrorLevel.MAJOR
            )
            send.buildErrorBack(error)
        }
        outChannel.send(back)
    }

    suspend fun processEvent(event: Event){
        processSend(event)
    }

}