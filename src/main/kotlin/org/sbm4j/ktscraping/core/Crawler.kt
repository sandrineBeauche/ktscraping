package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import org.kodein.di.*
import org.sbm4j.ktscraping.core.channels.ChannelFactory
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.Engine
import org.sbm4j.ktscraping.core.components.logger


interface CrawlerResult{

}

fun defaultDIModule(name: String): DI.Module {
    val mod = DI.Module(name = "defaultDIModule"){
            bind<Crawler> { multiton { di: DI -> DefaultCrawler(instance(), name, instance(), di) }}
            bindSingleton<Engine> { Engine(instance(), instance()) }
            bindSingleton<ChannelFactory> { ChannelFactory() }
            bindSingleton<ProgressMonitor> { ProgressMonitor() }
        }
    return mod
}


interface Crawler : Controllable, DIAware{

    val controllables: MutableList<Controllable>

    val channelFactory : ChannelFactory


    override suspend fun run() {
        for(cont in controllables){
            cont.start(this.scope)
        }
    }

    override suspend fun stop() {
        for(cont in controllables){
            cont.stop()
        }
        super.stop()
        try {
            this.scope.cancel()
        }
        catch(ex: CancellationException){
            logger.info { "Crawler stopped"  }
        }
    }

    suspend fun waitFinished(): CrawlerResult
}


abstract class AbstractCrawler(
    override val name: String = "AbstractCrawler",
    override val channelFactory: ChannelFactory,
): Crawler, AbstractControllable() {
    override val controllables: MutableList<Controllable> = mutableListOf()
}


class DefaultCrawler(
    channelFactory: ChannelFactory,
    name: String = "MainCrawler",
    val engine: Engine,
    override val di: DI
    ) : AbstractCrawler(name, channelFactory) {


    override suspend fun run() {
        logger.info{"${name}: Starting crawler"}
        engine.start(this.scope)
        super.run()
    }

    override suspend fun stop() {
        logger.info{ "${name}: Stopping crawler" }
        engine.stop()
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        engine.waitStarted()
        controllables.filterIsInstance<AbstractSpider>()
            .map { it.job }
            .joinAll()
        val result = engine.computeResult()
        return result
    }
}
