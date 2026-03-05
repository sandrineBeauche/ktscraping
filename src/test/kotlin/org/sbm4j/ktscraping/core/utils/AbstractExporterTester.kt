package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.logger
import kotlin.test.BeforeTest

abstract class AbstractExporterTester: ScrapingTest(){

    lateinit var exporter: AbstractExporter

    val exporterName: String = "Exporter"


    abstract fun buildExporter(exporterName: String): AbstractExporter

    @BeforeTest
    open fun setUp(){
        logger.debug { "setup abstractexporter tester" }
        exporter = buildExporter(exporterName)
        buildChannels()

        exporter.inChannel = inChannel
    }

    suspend fun withExporter(func: suspend AbstractExporterTester.() -> Unit){
        coroutineScope {
            initChannels(this)

            launch{
                exporter.start(this).join()
                doStartEvent()

                func()

                doEndEvent()

                exporter.stop()
                closeChannels()
            }
        }
    }
}