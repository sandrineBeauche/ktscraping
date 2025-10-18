package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest(){

    lateinit var spider: AbstractSpider

    lateinit var channel: SuperChannel

    val spiderName: String = "Spider"

    abstract fun buildSpider(spiderName: String): AbstractSpider

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        spider = buildSpider(spiderName)

        channel = SuperChannel()
        spider.outChannel = channel
    }

    suspend fun withSpider(func: suspend AbstractSpiderTester.() -> Unit){
        coroutineScope {
            channel.init()

            launch {
                spider.start(this)
            }
            launch{
                val startEvent = channel.receiveSend<StartEvent>()
                val startEventBack = startEvent.buildBack()
                channel.send(startEventBack)

                func()

                val endEvent = channel.receiveSend<EndEvent>()
                val endEventBack = endEvent.buildBack()
                channel.send(endEventBack)

                spider.job.join()

                channel.close()
                spider.stop()
            }


        }
    }
}