package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.kodein.di.*
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.components.Engine
import org.sbm4j.meercat.TopologyManager
import org.sbm4j.meercat.nodes.Controllable
import org.sbm4j.meercat.nodes.logger

/**
 * Marker interface for the result of a scraping session.
 *
 * Implementations carry the statistics and outcomes collected by the [AbstractEngine]
 * during a scraping session (e.g. [StatsCrawlerResult]). The result is computed by
 * [AbstractEngine.computeResult] at the end of the session and returned by
 * [Crawler.waitFinished].
 *
 * @see AbstractEngine
 * @see Crawler
 */
interface CrawlerResult{

}

/**
 * Default Kodein DI module for a standard KtScraping crawler.
 *
 * Binds all core components required to run a [DefaultCrawler]:
 * [Crawler], [Engine], [TopologyManager], [CrawlerChannelManager], and [ProgressMonitor].
 * All bindings use `multiton` scope, allowing multiple independent crawlers to run
 * simultaneously in the same JVM, each with their own isolated set of components.
 *
 * This module covers the vast majority of use cases without any customization.
 * For testing purposes, a dedicated test module is also available. Advanced users
 * may provide their own DI module to override specific bindings while keeping the rest.
 *
 * @param name The name of the crawler instance, passed to [DefaultCrawler].
 * @return A [DI.Module] ready to be installed in a Kodein [DI] container.
 *
 * @see Crawler
 * @see DefaultCrawler
 * @see Engine
 */
fun defaultDIModule(name: String): DI.Module {
    val mod = DI.Module(name = "defaultDIModule"){
            bind<Crawler> { multiton { di: DI -> DefaultCrawler(instance(arg = di), name, instance(arg = di), di) }}
            bind<Engine> { multiton{ di: DI -> Engine(instance(arg = di), instance(arg = di))} }
            bind<TopologyManager> { multiton{ di: DI -> TopologyManager(instance(arg = di))} }
            bind<CrawlerChannelManager> { multiton { di: DI -> CrawlerChannelManager() }}
            bind<ProgressMonitor> { multiton{di: DI ->  ProgressMonitor()} }
        }
    return mod
}

/**
 * Entry point for a KtScraping scraping session.
 *
 * A [Crawler] orchestrates the full lifecycle of a scraping session:
 * starting the topology via [Controllable], waiting for all Spiders to complete
 * via [waitFinished], and returning the final [CrawlerResult].
 *
 * Multiple [Crawler] instances can run simultaneously in the same JVM, each with
 * their own isolated [TopologyManager] and [CrawlerChannelManager].
 *
 * @see AbstractCrawler
 * @see DefaultCrawler
 * @see CrawlerResult
 */
interface Crawler : Controllable, DIAware{

    /**
     * Suspends until all Spiders have completed and returns the final [CrawlerResult].
     *
     * @return The aggregated result of the scraping session.
     */
    suspend fun waitFinished(): CrawlerResult

    /** The [TopologyManager] orchestrating the lifecycle of all topology nodes. */
    val topologyManager: TopologyManager

    /** The [CrawlerChannelManager] managing the three topology channels. */
    val channelManager: CrawlerChannelManager
}

/**
 * Base abstract class for [Crawler] implementations.
 *
 * Delegates [scope] and [start] to the [TopologyManager], and exposes the
 * [CrawlerChannelManager] from the topology's channel manager.
 *
 * @param name The name of this crawler instance.
 * @param topologyManager The [TopologyManager] orchestrating the topology lifecycle.
 *
 * @see Crawler
 * @see DefaultCrawler
 */
abstract class AbstractCrawler(
    val name: String = "AbstractCrawler",
    override val topologyManager: TopologyManager
): Crawler {

    override var scope: CoroutineScope
        get() = topologyManager.scope
        set(value) {}

    override val channelManager: CrawlerChannelManager
        get() = topologyManager.channelManager as CrawlerChannelManager

    override suspend fun start(parentScope: CoroutineScope, rootName: String): Job? {
        return topologyManager.start(parentScope, rootName)
    }
}

/**
 * Default concrete implementation of [Crawler] for standard scraping sessions.
 *
 * Registers the [Engine] in the [TopologyManager] and delegates the full scraping
 * lifecycle to it. [waitFinished] suspends until all Spiders have completed
 * (via [TopologyManager.waitCompleted]), then retrieves the final [CrawlerResult]
 * from [Engine.computeResult].
 *
 * Instantiated automatically by [defaultDIModule] — users typically do not
 * instantiate this class directly.
 *
 * @param topologyManager The [TopologyManager] orchestrating the topology lifecycle.
 * @param name The name of this crawler instance, defaults to `"MainCrawler"`.
 * @param engine The [Engine] node collecting statistics and computing the final result.
 * @param di The Kodein DI container for this crawler instance.
 *
 * @see AbstractCrawler
 * @see Engine
 * @see defaultDIModule
 */
class DefaultCrawler(
    topologyManager: TopologyManager,
    name: String = "MainCrawler",
    val engine: Engine,
    override val di: DI
    ) : AbstractCrawler(name, topologyManager) {

    init{
        topologyManager.nodes.add(engine)
    }

    override suspend fun start(parentScope: CoroutineScope, rootName: String): Job? {
        logger.info{"${name}: Starting crawler"}
        return super.start(parentScope, rootName)
    }


    override suspend fun stop() {
        logger.info{ "${name}: Stopping crawler" }
        super.stop()
    }

    /**
     * Suspends until all Spiders have completed, then computes and returns the
     * final [CrawlerResult] from the [Engine].
     *
     * @return The aggregated scraping statistics as a [CrawlerResult].
     */
    override suspend fun waitFinished(): CrawlerResult {
        logger.debug{ "${name}: Crawler started. Waiting for spiders finishing"}
        this.topologyManager.waitCompleted()
        logger.debug { "${name}: crawler finished all spiders, stop all" }
        val result = engine.computeResult()
        return result
    }
}
