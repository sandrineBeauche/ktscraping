package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.channels.SuperChannel

abstract class DualScrapingTest: ScrapingTest() {

    lateinit var outChannel: SuperChannel


    override fun initChannels(){
        super.initChannels()
        outChannel = SuperChannel()
    }

    override fun closeChannels(){
        super.closeChannels()
        outChannel.close()
    }


}