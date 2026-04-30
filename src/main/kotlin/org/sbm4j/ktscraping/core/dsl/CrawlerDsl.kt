package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.Crawler
import kotlin.reflect.full.primaryConstructor

/**
 * DSL entry point for building and configuring a KtScraping crawler.
 *
 * Creates a [Crawler] instance using the provided [diModuleFactory] to build the
 * Kodein DI module, then applies the [init] configuration block in the context of
 * the [Crawler]. Kodein is fully encapsulated here — users interact only with the
 * DSL and never with the DI container directly.
 *
 * Typical usage with the default module:
 * ```kotlin
 * val crawler = crawler("MyCrawler", ::defaultDIModule) {
 *     // configure spiders, middlewares, exporters...
 * }
 * ```
 *
 * Advanced usage with a custom module:
 * ```kotlin
 * val crawler = crawler("MyCrawler", { name -> myCustomDIModule(name) }) {
 *     // configure spiders, middlewares, exporters...
 * }
 * ```
 *
 * @param name The name of the crawler instance, defaults to `"Crawler"`.
 * @param diModuleFactory A factory function that produces the Kodein [DI.Module] for this
 * crawler. Pass [defaultDIModule] for standard use, or provide a custom factory to
 * override specific bindings.
 * @param init Configuration block applied to the [Crawler] instance after creation.
 * @return A fully configured [Crawler] ready to be started.
 *
 * @see defaultDIModule
 * @see Crawler
 */
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


/**
 * Reflective factory for instantiating [Component] nodes in the topology DSL.
 *
 * Uses Kotlin reflection to instantiate any [Component] subclass via its primary
 * constructor, optionally providing a [name] as the first parameter. If [name] is
 * `null` and the first parameter is optional, the component is created with all
 * defaults. If the first parameter is required, the class simple name is used as
 * the default name.
 *
 * This helper is used internally by the DSL to create topology nodes without
 * requiring the user to manage constructors directly.
 *
 * @param T The [Component] subclass to instantiate, reified.
 * @param name Optional name to pass as the first constructor parameter.
 * @return A new instance of [T].
 *
 * @see Component
 */
inline fun <reified T: Component> buildControllable(
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


