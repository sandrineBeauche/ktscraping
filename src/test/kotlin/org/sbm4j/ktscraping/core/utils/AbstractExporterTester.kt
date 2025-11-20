package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import kotlin.test.BeforeTest

abstract class AbstractExporterTester: ScrapingTest(){

    lateinit var exporter: AbstractExporter

    val exporterName: String = "Exporter"


    abstract fun buildExporter(exporterName: String): AbstractExporter

    @BeforeTest
    open fun setUp(){
        logger.debug { "setup abstractexporter tester" }
        exporter = buildExporter(exporterName)
        initChannels()

        exporter.inChannel = inChannel
    }

    suspend fun withExporter(func: suspend AbstractExporterTester.() -> Unit){
        coroutineScope {
            inChannel.init()

            launch{
                exporter.start(this)
                doStartEvent()

                func()

                doEndEvent()

                closeChannels()
                exporter.stop()
            }
        }
    }
}