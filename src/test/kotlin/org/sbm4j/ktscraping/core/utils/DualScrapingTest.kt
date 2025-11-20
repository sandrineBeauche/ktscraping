package org.sbm4j.ktscraping.core.utils

import io.mockk.mockk
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorLevel

abstract class DualScrapingTest: ScrapingTest() {

    lateinit var outChannel: SuperChannel

    val errorSender = mockk<Controllable>()


    override fun initChannels(){
        super.initChannels()
        outChannel = SuperChannel()
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
                ErrorLevel.MAJOR)
            send.buildErrorBack(error)
        }
        outChannel.send(back)
    }

    suspend fun processEvent(event: Event){
        processSend(event)
    }

}