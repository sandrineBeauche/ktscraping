package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.sync.Mutex
import org.kodein.di.*


interface CrawlerResult{

}

fun defaultDIModule(scope: CoroutineScope, name: String): DI.Module {
    val mod = DI.Module(name = "defaultDIModule"){
            bindSingleton<CoroutineScope> { scope }
            bind<Crawler> { multiton { di: DI -> DefaultCrawler(instance(), instance(), name, instance(), di) }}
            bindSingleton<Engine> { Engine(instance(), instance(), instance()) }
            bindSingleton<ChannelFactory> { ChannelFactory() }
            bindSingleton<ProgressMonitor> { ProgressMonitor() }
        }
    return mod
}


interface Crawler : Controllable, DIAware{

    val controllables: MutableList<Controllable>

    val channelFactory : ChannelFactory


    override suspend fun start() {
        for(cont in controllables){
            cont.start()
        }
    }

    override suspend fun stop() {
        for(cont in controllables){
            cont.stop()
        }
        super.stop()
    }

    suspend fun waitFinished(): CrawlerResult
}


abstract class AbstractCrawler(
    override val scope: CoroutineScope,
    override val name: String = "AbstractCrawler",
    override val channelFactory: ChannelFactory,
): Crawler {
    override val controllables: MutableList<Controllable> = mutableListOf()

    override val mutex: Mutex = Mutex()

    override var state: State = State()

}


class DefaultCrawler(
    scope: CoroutineScope,
    channelFactory: ChannelFactory,
    name: String = "MainCrawler",
    val engine: Engine,
    override val di: DI
    ) : AbstractCrawler(scope, name, channelFactory) {


    override suspend fun start() {
        logger.info{"${name}: Starting crawler"}
        engine.start()
        super.start()
    }

    override suspend fun stop() {
        logger.info{ "${name}: Stopping crawler" }
        engine.stop()
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        val result = (engine.resultChannel as ReceiveChannel<*>).receive()
        return result as CrawlerResult
    }
}
