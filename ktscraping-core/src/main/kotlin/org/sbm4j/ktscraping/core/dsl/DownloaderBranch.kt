package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.dispatchers.DownloaderDispatcher
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.meercat.channels.ChannelManager

/**
 * DSL function to configure a simple Downloader branch on this [Crawler].
 *
 * Creates a [DownloaderBranch] connected to [CrawlerChannelManager.downloaderChannel]
 * and applies the [initBranch] configuration block. All nodes created inside the block
 * are automatically registered in the [TopologyManager].
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     downloaderBranch {
 *         middleware<MyMiddleware>()
 *         downloader<MyDownloader>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [DownloaderBranch].
 *
 * @see DownloaderBranch
 * @see downloaderDispatcher
 */
fun Crawler.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val branch = DownloaderBranch(
        this.channelManager.downloaderChannel,
        this.di)
    branch.initBranch()
    this.topologyManager.nodes.addAll(branch.senders)
}

/**
 * DSL function to configure a [DownloaderDispatcher] directly on this [Crawler].
 *
 * Creates an anonymous [DownloaderDispatcher] connected to [CrawlerChannelManager.downloaderChannel],
 * with [selectChannelFunc] as the routing logic. Use this when multiple Downloader branches
 * are needed at the top level, each handling a different type of request.
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     downloaderDispatcher(
 *         selectChannelFunc = { request ->
 *             if (request is PlaywrightRequest) playwrightChannel else httpChannel
 *         }
 *     ) {
 *         downloader<HttpDownloader>()
 *         downloader<PlaywrightDownloader>()
 *     }
 * }
 * ```
 *
 * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
 * @param selectChannelFunc Routing function that maps each [AbstractRequest] to the
 * target [SuperChannel].
 * @param initDispatcher Configuration block applied to the [DownloaderDispatcher].
 *
 * @see DownloaderDispatcher
 * @see DownloaderBranch.downloaderDispatcher
 */
fun Crawler.downloaderDispatcher(
    name: String = "dispatcher",
    selectChannelFunc: DownloaderDispatcher.(AbstractRequest) -> SuperChannel,
    initDispatcher: DownloaderDispatcher.() -> Unit
){
    val dispatcher = object : DownloaderDispatcher(name, this.di){
        override fun selectChannel(request: AbstractRequest): SuperChannel {
            return selectChannelFunc(request)
        }
    }

    dispatcher.channelIn = this.channelManager.downloaderChannel

    dispatcher.initDispatcher()
    this.topologyManager.nodes.add(dispatcher)
}

/**
 * Represents a Downloader branch under construction in the DSL.
 *
 * Maintains the current end of the branch channel ([downloaderChannel]) and the list
 * of nodes created so far ([senders]). Each call to [middleware] or [downloader] extends
 * the branch by creating a new node, wiring its channels, and advancing [downloaderChannel]
 * to the new output channel.
 *
 * The DSL is recursive: a [DownloaderBranch] can contain a [downloaderDispatcher] which
 * itself contains further [DownloaderBranch]es or [downloader]s.
 *
 * @param downloaderChannel The current input channel for the next node in this branch.
 * @param di The Kodein DI container for this branch.
 *
 * @see Crawler.downloaderBranch
 * @see DownloaderDispatcher.downloaderBranch
 */
class DownloaderBranch(
    var downloaderChannel: SuperChannel,
    override val di: DI
): DIAware{
    /** Nodes created in this branch, to be registered in the [TopologyManager]. */
    val senders: MutableList<Component> = mutableListOf()

    /**
     * DSL function to add a middleware node to this Downloader branch.
     *
     * Instantiates [T] via [buildControllable], wires its input to the current
     * [downloaderChannel], creates a new output channel, and advances [downloaderChannel]
     * to it. Middlewares are chained in declaration order.
     *
     * @param T The [AbstractMiddleware] subclass to instantiate, reified.
     * @param name Optional name for this middleware node.
     * @param init Configuration block applied to the middleware instance.
     * @return The created middleware instance.
     */
    inline fun <reified T: AbstractMiddleware>middleware(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val mid = buildControllable<T>(name)

        senders.add(mid)
        mid.inChannel = downloaderChannel

        val channelManager: ChannelManager by di.instance(arg = this.di)
        val newChannel = channelManager.buildChannel()
        mid.outChannel = newChannel

        downloaderChannel = newChannel

        mid.init()
        return mid
    }

    /**
     * DSL function to add a terminal [AbstractDownloader] node to this branch.
     *
     * Instantiates [T] via [buildControllable] and wires its input to the current
     * [downloaderChannel]. A downloader is always the last node in a branch —
     * it has no output channel.
     *
     * @param T The [AbstractDownloader] subclass to instantiate, reified.
     * @param name Optional name for this downloader node.
     * @param init Configuration block applied to the downloader instance.
     * @return The created downloader instance.
     */
    inline fun <reified T: AbstractDownloader>downloader(
                                          name: String? = null,
                                          init: T.() -> Unit = {}): T{
        val down = buildControllable<T>(name)
        senders.add(down)

        down.inChannel = downloaderChannel
        down.init()

        return down
    }

    /**
     * DSL function to add a [DownloaderDispatcher] to this branch, enabling routing
     * to multiple sub-branches.
     *
     * Creates an anonymous [DownloaderDispatcher] connected to the current [downloaderChannel].
     * Use this to split the branch into multiple sub-branches, each handling a different
     * type of request. The DSL is recursive: each sub-branch can itself contain
     * middlewares, downloaders, or further dispatchers.
     *
     * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
     * @param selectChannelFunc Routing function that maps each [AbstractRequest] to the
     * target [SuperChannel].
     * @param init Configuration block applied to the [DownloaderDispatcher].
     */
    fun downloaderDispatcher(
        name: String = "dispatcher",
        selectChannelFunc: DownloaderDispatcher.(AbstractRequest) -> SuperChannel,
        init: DownloaderDispatcher.() -> Unit
    ){
        val dispatcher = object : DownloaderDispatcher(name, this.di){
            override fun selectChannel(request: AbstractRequest): SuperChannel {
                return selectChannelFunc(request)
            }
        }

        dispatcher.channelIn = downloaderChannel
        dispatcher.init()
        senders.add(dispatcher)
    }
}

/**
 * DSL function to add a terminal [AbstractDownloader] directly to a [DownloaderDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and wires it as the input of the new downloader. The downloader is registered in the
 * [TopologyManager] automatically.
 *
 * @param T The [AbstractDownloader] subclass to instantiate, reified.
 * @param name Optional name for this downloader node.
 * @param init Configuration block applied to the downloader instance.
 * @return The created downloader instance.
 *
 * @see DownloaderDispatcher
 * @see DownloaderDispatcher.downloaderBranch
 */
inline fun <reified T: AbstractDownloader> DownloaderDispatcher.downloader(
                                                                   name: String? = null,
                                                                   init: T.() -> Unit = {}): T {
    val down = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.topologyManager.nodes.add(down)

    val channel = crawler.channelManager.buildChannel()
    down.inChannel = channel

    this.addBranch(channel)
    down.init()

    return down
}

/**
 * DSL function to add a [DownloaderBranch] to a [DownloaderDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and applies [initBranch] to configure the branch. All nodes created inside the block
 * are registered in the [TopologyManager] automatically.
 *
 * This enables recursive DSL composition: a dispatcher can contain full branches,
 * each with their own middlewares, downloaders, or further dispatchers.
 *
 * Typical usage:
 * ```kotlin
 * downloaderDispatcher(selectChannelFunc = { ... }) {
 *     downloaderBranch {
 *         middleware<CacheMiddleware>()
 *         downloader<HttpDownloader>()
 *     }
 *     downloaderBranch {
 *         downloader<PlaywrightDownloader>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [DownloaderBranch].
 *
 * @see DownloaderDispatcher
 * @see DownloaderDispatcher.downloader
 */
fun DownloaderDispatcher.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val crawler : Crawler by this.di.instance(arg = this.di)

    val channel = crawler.channelManager.buildChannel()
    this.addBranch(channel)
    val branch = DownloaderBranch(channel, this.di)
    branch.initBranch()

    crawler.topologyManager.nodes.addAll(branch.senders)
}