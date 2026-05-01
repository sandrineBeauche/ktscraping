package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcher
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherAll
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherOne
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.channels.ChannelManager

/**
 * DSL function to configure a simple Pipeline branch on this [Crawler].
 *
 * Creates a [PipelineBranch] connected to [CrawlerChannelManager.pipelineChannel]
 * and applies the [initBranch] configuration block. All nodes created inside the block
 * are automatically registered in the [TopologyManager].
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     pipelineBranch {
 *         pipeline<MyPipeline>()
 *         exporter<MyExporter>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [PipelineBranch].
 *
 * @see PipelineBranch
 * @see pipelineDispatcherAll
 * @see pipelineDispatcherOne
 */
fun Crawler.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val branch = PipelineBranch(
        this.channelManager.pipelineChannel,
        this.di)
    branch.initBranch()
    this.topologyManager.nodes.addAll(branch.senders)
}

/**
 * DSL function to configure a [PipelineDispatcherAll] directly on this [Crawler].
 *
 * Creates a [PipelineDispatcherAll] connected to [CrawlerChannelManager.pipelineChannel]
 * that broadcasts every [Item] to all registered branches simultaneously.
 * Use this when items must be processed by all exporters at the same time.
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     pipelineDispatcherAll {
 *         exporter<DatabaseExporter>()
 *         exporter<CsvExporter>()
 *     }
 * }
 * ```
 *
 * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
 * @param initDispatcher Configuration block applied to the [PipelineDispatcherAll].
 *
 * @see PipelineDispatcherAll
 * @see pipelineDispatcherOne
 */
fun Crawler.pipelineDispatcherAll(name: String = "dispatcher", initDispatcher: PipelineDispatcherAll.() -> Unit){
    val dispatcher = PipelineDispatcherAll(name, this.di)
    dispatcher.channelIn = this.channelManager.pipelineChannel

    dispatcher.initDispatcher()
    this.topologyManager.nodes.add(dispatcher)
}

/**
 * DSL function to configure a [PipelineDispatcherOne] directly on this [Crawler].
 *
 * Creates an anonymous [PipelineDispatcherOne] connected to [CrawlerChannelManager.pipelineChannel]
 * that routes each [Item] to a single branch based on [selectChannelFunc].
 * Use this when different item types must be routed to different exporters.
 *
 * Typical usage:
 * ```kotlin
 * crawler("MyCrawler", ::defaultDIModule) {
 *     pipelineDispatcherOne(
 *         selectChannelFunc = { item ->
 *             if (item.label == "product") productChannel else articleChannel
 *         }
 *     ) {
 *         exporter<ProductExporter>()
 *         exporter<ArticleExporter>()
 *     }
 * }
 * ```
 *
 * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
 * @param selectChannelFunc Routing function that maps each [Item] to the target [SuperChannel].
 * @param init Configuration block applied to the [PipelineDispatcherOne].
 *
 * @see PipelineDispatcherOne
 * @see pipelineDispatcherAll
 */
fun Crawler.pipelineDispatcherOne(
    name: String = "dispatcher",
    selectChannelFunc: PipelineDispatcherOne.(Item) -> SuperChannel,
    init: PipelineDispatcherOne.() -> Unit
){
    val dispatcher = object : PipelineDispatcherOne(name, this.di){
        override fun selectChannel(item: Item): SuperChannel {
            return selectChannelFunc(item)
        }
    }

    dispatcher.channelIn = this.channelManager.pipelineChannel

    dispatcher.init()
    this.topologyManager.nodes.add(dispatcher)
}

/**
 * Represents a Pipeline branch under construction in the DSL.
 *
 * Maintains the current end of the branch channel ([pipelineChannel]) and the list
 * of nodes created so far ([senders]). Each call to [pipeline] or [exporter] extends
 * the branch by creating a new node, wiring its channels, and advancing [pipelineChannel]
 * to the new output channel.
 *
 * The DSL is recursive: a [PipelineBranch] can contain a [pipelineDispatcherAll] or
 * [pipelineDispatcherOne] which themselves contain further [PipelineBranch]es or
 * [exporter]s.
 *
 * @param pipelineChannel The current input channel for the next node in this branch.
 * @param di The Kodein DI container for this branch.
 *
 * @see Crawler.pipelineBranch
 * @see PipelineDispatcher.pipelineBranch
 */
class PipelineBranch(
    var pipelineChannel: SuperChannel,
    override val di: DI
): DIAware{

    /** Nodes created in this branch, to be registered in the [TopologyManager]. */
    val senders : MutableList<Component> = mutableListOf()

    /**
     * DSL function to add an intermediate [AbstractPipeline] node to this branch.
     *
     * Instantiates [T] via [buildControllable], wires its input to the current
     * [pipelineChannel], creates a new output channel, and advances [pipelineChannel]
     * to it. Pipeline nodes are chained in declaration order.
     *
     * @param T The [AbstractPipeline] subclass to instantiate, reified.
     * @param name Optional name for this pipeline node.
     * @param init Configuration block applied to the pipeline instance.
     * @return The created pipeline instance.
     */
    inline fun <reified T: AbstractPipeline>pipeline(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T{
        val pip = buildControllable<T>(name)

        senders.add(pip)
        pip.inChannel = pipelineChannel

        val channelManager: ChannelManager by di.instance(arg = this.di)
        val newChannel = channelManager.buildChannel()
        pip.outChannel = newChannel
        pipelineChannel = newChannel

        pip.init()

        return pip
    }

    /**
     * DSL function to add a terminal [AbstractExporter] node to this branch.
     *
     * Instantiates [T] via [buildControllable] and wires its input to the current
     * [pipelineChannel]. An exporter is always the last node in a branch —
     * it has no output channel.
     *
     * @param T The [AbstractExporter] subclass to instantiate, reified.
     * @param name Optional name for this exporter node.
     * @param init Configuration block applied to the exporter instance.
     * @return The created exporter instance.
     */
    inline fun <reified T: AbstractExporter>exporter(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T {
        val exp = buildControllable<T>(name)

        senders.add(exp)

        exp.inChannel = pipelineChannel
        exp.init()

        return exp
    }

    /**
     * DSL function to add a [PipelineDispatcherAll] to this branch, broadcasting
     * items to all sub-branches simultaneously.
     *
     * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
     * @param init Configuration block applied to the [PipelineDispatcherAll].
     */
    fun pipelineDispatcherAll(name: String = "dispatcher", init: PipelineDispatcherAll.() -> Unit){
        val dispatcher = PipelineDispatcherAll(name, this.di)
        dispatcher.channelIn = pipelineChannel
        dispatcher.init()
        senders.add(dispatcher)
    }

    /**
     * DSL function to add a [PipelineDispatcherOne] to this branch, routing each
     * item to a single sub-branch based on [selectChannelFunc].
     *
     * @param name The name of the dispatcher node, defaults to `"dispatcher"`.
     * @param selectChannelFunc Routing function that maps each [Item] to the target [SuperChannel].
     * @param init Configuration block applied to the [PipelineDispatcherOne].
     */
    fun pipelineDispatcherOne(
        name: String = "dispatcher",
        selectChannelFunc: PipelineDispatcherOne.(Item) -> SuperChannel,
        init: PipelineDispatcherOne.() -> Unit
    ){
        val dispatcher = object : PipelineDispatcherOne(name, this.di){
            override fun selectChannel(item: Item): SuperChannel {
                return selectChannelFunc(item)
            }
        }

        dispatcher.channelIn = pipelineChannel
        dispatcher.init()
        senders.add(dispatcher)
    }
}

/**
 * DSL function to add a terminal [AbstractExporter] directly to a [PipelineDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and wires it as the input of the new exporter. The exporter is registered in the
 * [TopologyManager] automatically.
 *
 * @param T The [AbstractExporter] subclass to instantiate, reified.
 * @param name Optional name for this exporter node.
 * @param init Configuration block applied to the exporter instance.
 * @return The created exporter instance.
 *
 * @see PipelineDispatcher
 * @see PipelineDispatcher.pipelineBranch
 */
inline fun <reified T: AbstractExporter> PipelineDispatcher.exporter(
                                                  name: String? = null,
                                                  init: T.() -> Unit = {}): T? {
    val exp = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.topologyManager.nodes.add(exp)

    val newChannel = crawler.channelManager.buildChannel()

    exp.inChannel = newChannel

    this.addBranch(newChannel)
    exp.init()

    return exp
}

/**
 * DSL function to add a [PipelineBranch] to a [PipelineDispatcher].
 *
 * Creates a new [SuperChannel], registers it as a branch of the dispatcher via [addBranch],
 * and applies [initBranch] to configure the branch. All nodes created inside the block
 * are registered in the [TopologyManager] automatically.
 *
 * This enables recursive DSL composition: a dispatcher can contain full branches,
 * each with their own pipeline nodes, exporters, or further dispatchers.
 *
 * Typical usage:
 * ```kotlin
 * pipelineDispatcherAll {
 *     pipelineBranch {
 *         pipeline<ValidationPipeline>()
 *         exporter<DatabaseExporter>()
 *     }
 *     pipelineBranch {
 *         exporter<CsvExporter>()
 *     }
 * }
 * ```
 *
 * @param initBranch Configuration block applied to the [PipelineBranch].
 *
 * @see PipelineDispatcher
 * @see PipelineDispatcher.exporter
 */
fun PipelineDispatcher.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val crawler : Crawler by this.di.instance(arg = this.di)
    val newChannel = crawler.channelManager.buildChannel()
    this.addBranch(newChannel)
    val branch = PipelineBranch(newChannel, this.di)
    branch.initBranch()

    crawler.topologyManager.nodes.addAll(branch.senders)
}