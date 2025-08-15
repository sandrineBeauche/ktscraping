package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.SuperChannel

abstract class DualScrapingTest<IN, OUT>: ScrapingTest() {

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