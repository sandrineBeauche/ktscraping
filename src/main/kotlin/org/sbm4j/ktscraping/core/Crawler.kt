package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import org.kodein.di.*
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.components.Engine
import org.sbm4j.meercat.TopologyManager
import org.sbm4j.meercat.channels.ChannelManager
import org.sbm4j.meercat.nodes.Controllable
import org.sbm4j.meercat.nodes.logger


interface CrawlerResult{

}

fun defaultDIModule(name: String): DI.Module {
    val mod = DI.Module(name = "defaultDIModule"){
            bind<Crawler> { multiton { di: DI -> DefaultCrawler(instance(arg = di), name, instance(), di) }}
            bindSingleton<Engine> { Engine(instance(), instance()) }
            bind<CrawlerChannelManager> { multiton { di: DI -> CrawlerChannelManager() }}
            bindSingleton<ProgressMonitor> { ProgressMonitor() }
        }
    return mod
}


interface Crawler : Controllable, DIAware{

    suspend fun waitFinished(): CrawlerResult

    val topologyManager: TopologyManager

    val channelManager: CrawlerChannelManager
}


abstract class AbstractCrawler(
    val name: String = "AbstractCrawler",
    override val channelManager: CrawlerChannelManager,
): Crawler {

    override lateinit var scope: CoroutineScope

    override val topologyManager: TopologyManager = TopologyManager(channelManager)

    override suspend fun start(parentScope: CoroutineScope, rootName: String): Job? {
        return super.start(parentScope, "${name}-root")
    }
}


class DefaultCrawler(
    crawlerChannelManager: CrawlerChannelManager,
    name: String = "MainCrawler",
    val engine: Engine,
    override val di: DI
    ) : AbstractCrawler(name, crawlerChannelManager) {



    override suspend fun start(parentScope: CoroutineScope, rootName: String): Job? {
        logger.info{"${name}: Starting crawler"}
        super.start(parentScope, rootName)
        engine.start(this.scope)
        this.topologyManager.start(parentScope, rootName)
        return null
    }


    override suspend fun stop() {
        logger.info{ "${name}: Stopping crawler" }
        engine.stop()
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        engine.waitStarted()
        logger.debug{ "${name}: Crawler started. Waiting for spiders finishing"}
        this.topologyManager.waitCompleted()
        logger.debug { "${name}: crawler finished all spiders, stop all" }
        val result = engine.computeResult()
        return result
    }
}
