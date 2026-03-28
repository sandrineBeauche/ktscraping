package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.item.Item
import kotlin.test.BeforeTest

abstract class AbstractPipelineTester: DualScrapingTest() {

    lateinit var pipeline: AbstractPipeline

    val pipelineName: String = "Pipeline"

    abstract fun buildPipeline(pipelineName: String): AbstractPipeline

    @BeforeTest
    open fun setUp(){
        buildChannels()
        clearAllMocks()

        pipeline = buildPipeline(pipelineName)

        pipeline.inChannel = inChannel
        pipeline.outChannel = outChannel
    }


    suspend fun processItem(item: Item){
        val back = item.buildBack()
        outChannel.send(back)
    }

    suspend fun withPipeline(nbMessages: Int = 1, func: suspend AbstractPipelineTester.() -> Unit) {
        coroutineScope {
            initChannels(this)

            launch {
                pipeline.start(this)?.join()

                doStartEvent()
                func()

                doEndEvent()
                pipeline.stop()
                closeChannels()
            }
            launch {
                outChannel.getSendFlow().take(nbMessages + 2).collect { send ->
                    when(send){
                        is StartEvent, is EndEvent -> {
                            val back = send.buildBack()
                            outChannel.send(back)
                        }
                        is Event -> {
                            processEvent(send)
                        }
                        is Item -> {
                            processItem(send)
                        }
                    }
                }
            }
        }
    }
}