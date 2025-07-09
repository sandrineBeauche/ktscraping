package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.channels.Channel

abstract class DualScrapingTest<IN, OUT>: ScrapingTest<IN, OUT>() {

    lateinit var forwardInChannel: Channel<IN>

    lateinit var forwardOutChannel: Channel<OUT>

    override fun initChannels(){
        super.initChannels()
        forwardInChannel = Channel<IN>(Channel.UNLIMITED)
        forwardOutChannel = Channel<OUT>(Channel.UNLIMITED)
    }

    override fun closeChannels(){
        super.closeChannels()
        forwardInChannel.close()
        forwardOutChannel.close()
    }
}