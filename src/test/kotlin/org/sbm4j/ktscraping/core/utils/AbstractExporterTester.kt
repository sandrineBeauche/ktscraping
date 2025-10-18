package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import kotlin.test.BeforeTest

abstract class AbstractExporterTester: ScrapingTest(){

    lateinit var exporter: AbstractExporter

    val exporterName: String = "Exporter"

    val sender: Controllable = mockk<Controllable>()

    abstract fun buildExporter(exporterName: String): AbstractExporter

    @BeforeTest
    open fun setUp(){
        logger.debug { "setup abstractexporter tester" }
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        exporter = spyk(buildExporter(exporterName))

        every { exporter.inChannel } returns inChannel
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