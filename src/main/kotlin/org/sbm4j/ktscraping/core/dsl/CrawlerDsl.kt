package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.DefaultCrawler
import kotlin.reflect.full.primaryConstructor


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



inline fun <reified T: Controllable> buildControllable(
    name: String? = null,
    scope: CoroutineScope
): T {
    val construct = T::class.primaryConstructor!!
    val params = construct.parameters
    val values = if(name == null){
        if(params[1].isOptional) {
            mapOf(params[0] to scope)
        }
        else{
            mapOf(params[0] to scope, params[1] to T::class.simpleName)
        }
    }
    else{
        mapOf(params[0] to scope, params[1] to name)
    }
    return construct.callBy(values)
}


