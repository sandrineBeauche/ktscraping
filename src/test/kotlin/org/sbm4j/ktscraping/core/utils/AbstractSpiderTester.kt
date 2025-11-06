package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest(){

    lateinit var spider: AbstractSpider

    lateinit var outChannel: SuperChannel

    val spiderName: String = "Spider"

    abstract fun buildSpider(spiderName: String): AbstractSpider

    override fun initChannels() {
        super.initChannels()
        outChannel = SuperChannel()
    }

    override fun closeChannels() {
        super.closeChannels()
        outChannel.close()
    }

    @BeforeTest
    fun setUp(){
        initChannels()

        spider = buildSpider(spiderName)
        spider.outChannel = outChannel
    }

    override suspend fun doStartEvent() {
        val startEvent = outChannel.receiveSend<StartEvent>()

    }

    override suspend fun doEndEvent() {
        logger.debug { "Waiting for end event..." }
        val endEvent = outChannel.receiveSend<EndEvent>()

    }

    suspend fun withSpider(nbMessages: Int = 1, func: suspend AbstractSpiderTester.(send: Send) -> Unit){
        coroutineScope {
            outChannel.init()

            launch {
                spider.start(this)

                spider.job.join()
                outChannel.close()
                spider.stop()
                logger.debug { "Spider is stopped" }
            }
            launch{
                outChannel.getSendFlow().take(nbMessages + 2).collect { send ->
                    when(send){
                        is StartEvent -> {
                            logger.debug{ "received a start event and send a back" }
                            val startEventBack = send.buildBack()
                            outChannel.send(startEventBack)
                        }
                        is EndEvent -> {
                            logger.debug{ "received a end event and send a back" }
                            val endEventBack = send.buildBack()
                            outChannel.send(endEventBack)
                        }
                        else -> {
                            func(send)
                        }
                    }
                }
            }
        }
    }
}