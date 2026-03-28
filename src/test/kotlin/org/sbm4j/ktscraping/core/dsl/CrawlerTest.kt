package org.sbm4j.ktscraping.core.dsl

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import org.kodein.di.*
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource


class TestingCrawlerResult: CrawlerResult

class EmptyTestingCrawler(
    name: String = "TestCrawler",
    crawlerChannelManager: CrawlerChannelManager,
    override val di: DI
) : AbstractCrawler(name, crawlerChannelManager){

    override suspend fun start(parentScope: CoroutineScope, rootName: String): Job? {
        logger.info{"Starting testing crawler ${name}"}
        return super.start(parentScope, rootName)
    }


    override suspend fun stop() {
        logger.info{"Stopping testing crawler ${name}"}
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        topologyManager.waitCompleted()
        return TestingCrawlerResult()
    }
}


abstract class CrawlerTest {

    val scope = TestScope()

    val sender: SendSource = mockk<SendSource>()

    val crawlerChannelManager : CrawlerChannelManager = CrawlerChannelManager()

    fun testDIModule(name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bind<Crawler> { multiton { di: DI -> EmptyTestingCrawler(name, instance(arg = di), di) }}
            bind<CrawlerChannelManager> { multiton { di: DI -> crawlerChannelManager }}
        }
        return mod
    }
}