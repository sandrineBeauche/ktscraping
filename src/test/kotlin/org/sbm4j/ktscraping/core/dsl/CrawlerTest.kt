package org.sbm4j.ktscraping.core.dsl

import io.mockk.mockk
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import org.kodein.di.*
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.channels.ChannelManager
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.meercat.components.SendSource
import org.sbm4j.meercat.components.logger


class TestingCrawlerResult: CrawlerResult

class EmptyTestingCrawler(
    name: String = "TestCrawler",
    channelManager: ChannelManager,
    override val di: DI
) : AbstractCrawler(name, channelManager){

    override suspend fun run() {
        logger.info{"Starting testing crawler ${name}"}
        super.run()
    }

    override suspend fun stop() {
        logger.info{"Stopping testing crawler ${name}"}
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        controllables.filterIsInstance<AbstractSpider>()
            .map { it.job!! }
            .joinAll()
        return TestingCrawlerResult()
    }
}


abstract class CrawlerTest {

    val scope = TestScope()

    val sender: SendSource = mockk<SendSource>()

    val channelManager : ChannelManager = ChannelManager()

    fun testDIModule(name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bind<Crawler> { multiton { di: DI -> EmptyTestingCrawler(name, instance(arg = di), di) }}
            bind<ChannelManager> { multiton {di: DI -> channelManager }}
        }
        return mod
    }
}