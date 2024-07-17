package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.requests.Item
import kotlin.test.BeforeTest

abstract class AbstractExporterTester: ScrapingTest<Item, Item>(){

    lateinit var exporter: AbstractExporter

    val exporterName: String = "Exporter"

    abstract fun buildExporter(sc: CoroutineScope, exporterName: String): AbstractExporter

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        exporter = spyk(buildExporter(sc, exporterName))

        every { exporter.itemIn } returns inChannel
    }

    suspend fun withExporter(func: suspend AbstractExporterTester.() -> Unit){
        coroutineScope {
            every { exporter.scope } returns this
            exporter.start()

            func()

            closeChannels()
            exporter.stop()
        }
    }
}