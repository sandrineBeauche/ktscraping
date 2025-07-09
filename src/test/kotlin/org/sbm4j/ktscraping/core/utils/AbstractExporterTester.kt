package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import kotlin.test.BeforeTest

abstract class AbstractExporterTester: ScrapingTest<Item, ItemAck>(){

    lateinit var exporter: AbstractExporter

    val exporterName: String = "Exporter"

    abstract fun buildExporter(exporterName: String): AbstractExporter

    @BeforeTest
    open fun setUp(){
        logger.debug { "setup abstractexporter tester" }
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        exporter = spyk(buildExporter(exporterName))

        every { exporter.itemIn } returns inChannel
        every { exporter.itemAckOut } returns outChannel
    }

    suspend fun withExporter(func: suspend AbstractExporterTester.() -> Unit){
        coroutineScope {
            every { exporter.scope } returns this
            exporter.start(this)

            func()

            closeChannels()
            exporter.stop()
        }
    }
}