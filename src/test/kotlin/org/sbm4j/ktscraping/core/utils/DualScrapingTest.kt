package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.channels.Channel

abstract class DualScrapingTest<IN, OUT>: ScrapingTest<IN, OUT>() {

    lateinit var followInChannel: Channel<IN>

    lateinit var followOutChannel: Channel<OUT>

    override fun initChannels(){
        super.initChannels()
        followInChannel = Channel<IN>(Channel.UNLIMITED)
        followOutChannel = Channel<OUT>(Channel.UNLIMITED)
    }

    override fun closeChannels(){
        super.closeChannels()
        followInChannel.close()
        followOutChannel.close()
    }
}