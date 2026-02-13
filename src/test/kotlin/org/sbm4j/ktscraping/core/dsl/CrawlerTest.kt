package org.sbm4j.ktscraping.core.dsl

import io.mockk.mockk
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import org.kodein.di.*
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.channels.ChannelFactory
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.item.Data




class TestingCrawlerResult: CrawlerResult

class EmptyTestingCrawler(
    name: String = "TestCrawler",
    channelFactory: ChannelFactory,
    override val di: DI
) : AbstractCrawler(name, channelFactory){

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
            .map { it.job }
            .joinAll()
        return TestingCrawlerResult()
    }
}


abstract class CrawlerTest {

    val scope = TestScope()

    val sender: Controllable = mockk<Controllable>()

    val channelFactory : ChannelFactory = ChannelFactory()

    fun testDIModule(name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bind<Crawler> { multiton { di: DI -> EmptyTestingCrawler(name, instance(), di) }}
            bindSingleton<ChannelFactory> { channelFactory }
        }
        return mod
    }
}