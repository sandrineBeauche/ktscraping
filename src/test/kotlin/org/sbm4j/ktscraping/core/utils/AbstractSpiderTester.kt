package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.meercat.components.logger
import org.sbm4j.meercat.channels.Send
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.BeforeTest

abstract class AbstractSpiderTester: ScrapingTest(){

    lateinit var spider: AbstractSpider

    lateinit var outChannel: SuperChannel

    val spiderName: String = "Spider"

    abstract fun buildSpider(spiderName: String): AbstractSpider

    override fun buildChannels() {
        outChannel = SuperChannel()
    }

    override fun initChannels(parentScope: CoroutineScope) {
        outChannel.init(parentScope)
    }

    override fun closeChannels() {
        outChannel.close()
    }

    @BeforeTest
    fun setUp(){
        buildChannels()

        spider = buildSpider(spiderName)
        spider.outChannel = outChannel
    }


    suspend fun withSpider(nbMessages: Int = 1, func: suspend AbstractSpiderTester.(send: Send) -> Unit){
        coroutineScope {
            launch(CoroutineName("Testing-container-scope")) {
                initChannels(this)

                coroutineScope {
                    launch(CoroutineName("messages-receiver")) {
                        outChannel.getSendFlow().take(nbMessages + 2).collect { send ->
                            when (send) {
                                is StartEvent -> {
                                    logger.debug { "received a start event and send a back" }
                                    val startEventBack = send.buildBack()
                                    outChannel.send(startEventBack)
                                }

                                is EndEvent -> {
                                    logger.debug { "received a end event and send a back" }
                                    val endEventBack = send.buildBack()
                                    outChannel.send(endEventBack)
                                }

                                else -> {
                                    func(send)
                                }
                            }
                        }
                        spider.job.join()
                        spider.stop()
                        logger.debug { "Spider is stopped" }
                    }
                    launch(CoroutineName("component-run")) {
                        spider.start(this).join()
                        logger.debug { "finished to start spider" }


                    }
                }

                closeChannels()
            }
        }
    }
}