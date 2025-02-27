package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.DefaultCrawler
import kotlin.reflect.full.primaryConstructor


fun crawler(name: String = "Crawler",
            diModuleFactory: (String) -> DI.Module,
            init: Crawler.() -> Unit): Crawler {
    val di = DI{
        import(diModuleFactory(name))
    }
    val result : Crawler by di.instance(arg = di)
    result.init()
    return result
}



inline fun <reified T: Controllable> buildControllable(
    name: String? = null,
): T {
    val construct = T::class.primaryConstructor!!
    val params = construct.parameters
    val values = if(name == null){
        if(params[0].isOptional) {
            mapOf()
        }
        else{
            mapOf(params[0] to T::class.simpleName)
        }
    }
    else{
        mapOf(params[0] to name)
    }
    return construct.callBy(values)
}


