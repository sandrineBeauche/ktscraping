package org.sbm4j.ktscraping.core.dsl

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import org.kodein.di.*
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.TopologyManager
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource


class TestingCrawlerResult: CrawlerResult

class EmptyTestingCrawler(
    name: String = "TestCrawler",
    topologyManager: TopologyManager,
    override val di: DI
) : AbstractCrawler(name, topologyManager) {

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

    val topologyManager: TopologyManager = TopologyManager(crawlerChannelManager)

    fun testDIModule(name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bind<Crawler> { multiton { di: DI -> EmptyTestingCrawler(name, instance(arg = di), di) }}
            bind<CrawlerChannelManager> { multiton { di: DI -> crawlerChannelManager }}
            bind<TopologyManager> {multiton { di: DI -> topologyManager }}
        }
        return mod
    }

    suspend fun sendStartEvent(channel: SuperChannel){
        val startEvent = StartEvent(sender)
        val back = channel.sendSync<EventBack>(startEvent)
    }

    suspend fun sendEndEvent(channel: SuperChannel){
        val endEvent = EndEvent(sender)
        val back = channel.sendSync<EventBack>(endEvent)
    }


}