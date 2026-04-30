package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.dispatchers.SpiderDispatcher

/**
 * DSL function to configure a simple Spider branch on this [Crawler].
 *
 * Creates a [SpiderBranch] connected to [CrawlerChannelManager.spiderChannel]
 * and applies the [initBranch] configuration block. All nodes created inside the block
 * are automatically registered in the [TopologyManager].
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     spiderBranch {
 *         spiderMiddleware<MyMiddleware>()
 *         spider<MySpider>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [SpiderBranch].
 *
 * @see SpiderBranch
 * @see spiderDispatcher
 */
fun Crawler.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val branch = SpiderBranch(
        this.channelManager.spiderChannel,
        this.di)
    branch.initBranch()
    this.topologyManager.nodes.addAll(branch.senders)
}

/**
 * DSL function to configure a [SpiderDispatcher] directly on this [Crawler].
 *
 * Creates a [SpiderDispatcher] whose output is connected to [CrawlerChannelManager.spiderChannel],
 * collecting messages from multiple Spider branches into a single channel toward the Engine.
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     spiderDispatcher {
 *         spider<SpiderA>()
 *         spider<SpiderB>()
 *     }
 * }
 * ```
 *
 * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
 * @param initDispatcher Configuration block applied to the [SpiderDispatcher].
 *
 * @see SpiderDispatcher
 * @see SpiderBranch
 */
fun Crawler.spiderDispatcher(name: String = "dispatcher", initDispatcher: SpiderDispatcher.() -> Unit){
    val dispatcher = SpiderDispatcher(name, this.di)
    dispatcher.channelOut = this.channelManager.spiderChannel

    dispatcher.initDispatcher()
    this.topologyManager.nodes.add(dispatcher)
}


/**
 * Represents a Spider branch under construction in the DSL.
 *
 * Unlike [DownloaderBranch] and [PipelineBranch] where channels advance forward
 * (from source to sink), [SpiderBranch] advances **backward**: the Spider is the
 * [Initiator] at the end of the chain and pushes messages toward the Engine via
 * [CrawlerChannelManager.spiderChannel]. Each [spiderMiddleware] call creates a new
 * input channel and advances [channel] toward the Spider.
 *
 * The DSL is recursive: a [SpiderBranch] can contain a [spiderDispatcher] which
 * itself contains further [SpiderBranch]es or [spider]s.
 *
 * @param channel The current output channel for the next node in this branch.
 * @param di The Kodein DI container for this branch.
 *
 * @see Crawler.spiderBranch
 * @see SpiderDispatcher.spiderBranch
 */
class SpiderBranch(
    var channel: SuperChannel,
    override val di: DI
) : DIAware {

    /** Nodes created in this branch, to be registered in the [TopologyManager]. */
    val senders : MutableList<Component> = mutableListOf()

    val crawlerChannelManager: CrawlerChannelManager by di.instance(arg = di)

    /**
     * DSL function to add a [SpiderMiddleware] node to this Spider branch.
     *
     * Instantiates [T] via [buildControllable], wires its output to the current [channel],
     * creates a new input channel, and advances [channel] to it. Middlewares are chained
     * in declaration order, between the Spider and the Engine.
     *
     * Note: wiring is reversed compared to Downloader/Pipeline middlewares —
     * [outChannel] points toward the Engine and [inChannel] points toward the Spider.
     *
     * @param T The [SpiderMiddleware] subclass to instantiate, reified.
     * @param name Optional name for this middleware node.
     * @param init Configuration block applied to the middleware instance.
     * @return The created middleware instance.
     */
    inline fun <reified T : SpiderMiddleware>spiderMiddleware(
                                               name: String? = null,
                                               init: T.() -> Unit = {}): T{
        val mid = buildControllable<T>(name)

        senders.add(mid)

        mid.outChannel = channel
        val newChannel = crawlerChannelManager.buildChannel()
        mid.inChannel = newChannel
        channel = newChannel

        mid.init()

        return mid
    }

    /**
     * DSL function to add a terminal [AbstractSpider] node to this branch.
     *
     * Instantiates [T] via [buildControllable] and wires its output to the current [channel].
     * A spider is always the last node declared in a branch — it has no input channel
     * as it is the [Initiator] of the topology.
     *
     * @param T The [AbstractSpider] subclass to instantiate, reified.
     * @param name Optional name for this spider node.
     * @param init Configuration block applied to the spider instance.
     * @return The created spider instance.
     */
    inline fun <reified T: AbstractSpider>spider(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val spid = buildControllable<T>(name)

        senders.add(spid)
        spid.outChannel = channel

        spid.init()
        return spid
    }

    /**
     * DSL function to add a [SpiderDispatcher] to this branch, collecting messages
     * from multiple Spider sub-branches into a single channel.
     *
     * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
     * @param init Configuration block applied to the [SpiderDispatcher].
     */
    fun spiderDispatcher(name: String = "dispatcher", init: SpiderDispatcher.() -> Unit){
        val dispatcher = SpiderDispatcher(name, this.di)
        dispatcher.channelOut = channel
        dispatcher.init()
        senders.add(dispatcher)
    }

}


/**
 * DSL function to add a terminal [AbstractSpider] directly to a [SpiderDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and wires it as the output of the new spider. The spider is registered in the
 * [TopologyManager] automatically.
 *
 * @param T The [AbstractSpider] subclass to instantiate, reified.
 * @param name Optional name for this spider node.
 * @param init Configuration block applied to the spider instance.
 * @return The created spider instance.
 *
 * @see SpiderDispatcher
 * @see SpiderDispatcher.spiderBranch
 */
inline fun <reified T: AbstractSpider> SpiderDispatcher.spider(
    name: String? = null,
    init: T.() -> Unit = {}
): T {
    val spid = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.topologyManager.nodes.add(spid)

    val newChannel = crawler.channelManager.buildChannel()
    spid.outChannel = newChannel

    this.addBranch(newChannel)
    spid.init()

    return spid
}

/**
 * DSL function to add a [SpiderBranch] to a [SpiderDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and applies [initBranch] to configure the branch. All nodes created inside the block
 * are registered in the [TopologyManager] automatically.
 *
 * This enables recursive DSL composition: a dispatcher can contain full branches,
 * each with their own middlewares, spiders, or further dispatchers.
 *
 * Typical usage:
 * ```kotlin
 * spiderDispatcher {
 *     spiderBranch {
 *         spiderMiddleware<AuthMiddleware>()
 *         spider<ProductSpider>()
 *     }
 *     spiderBranch {
 *         spider<ArticleSpider>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [SpiderBranch].
 *
 * @see SpiderDispatcher
 * @see SpiderDispatcher.spider
 */
fun SpiderDispatcher.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val crawler : Crawler by this.di.instance(arg = this.di)

    val channel = crawler.channelManager.buildChannel()
    this.addBranch(channel)
    val branch = SpiderBranch(channel, this.di)
    branch.initBranch()

    crawler.topologyManager.nodes.addAll(branch.senders)
}