package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.CrawlerConfiguration
import org.sbm4j.ktscraping.core.DefaultCrawler


fun crawler(scope: CoroutineScope,
            name: String = "Crawler",
            diModuleFactory: (CoroutineScope, String) -> DI.Module,
            init: Crawler.() -> Unit): Crawler {
    val di = DI{
        import(diModuleFactory(scope, name))
    }
    val result : Crawler by di.instance(arg = di)
    result.init()
    return result
}


fun DefaultCrawler.configuration(initConf: CrawlerConfiguration.() -> Unit){
    this.configuration.initConf()
}


