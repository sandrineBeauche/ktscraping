package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import kotlin.test.BeforeTest

abstract class AbstractPipelineTester: DualScrapingTest<Item, ItemAck>() {

    lateinit var pipeline: AbstractPipeline

    val pipelineName: String = "Pipeline"

    abstract fun buildPipeline(pipelineName: String): AbstractPipeline

    @BeforeTest
    open fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        pipeline = spyk(buildPipeline(pipelineName))

        every { pipeline.itemIn } returns inChannel
        every { pipeline.itemOut } returns forwardInChannel
        every { pipeline.itemAckIn } returns outChannel
        every { pipeline.itemAckOut } returns forwardOutChannel
    }

    suspend fun withPipeline(func: suspend AbstractPipelineTester.() -> Unit){
        coroutineScope {
            every { pipeline.scope } returns this
            pipeline.start(this)

            func()

            closeChannels()
            pipeline.stop()
        }
    }
}