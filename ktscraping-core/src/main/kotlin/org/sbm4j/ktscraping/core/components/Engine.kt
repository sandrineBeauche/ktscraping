package org.sbm4j.ktscraping.core.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.core.ProgressMonitor
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.ktscraping.core.processors.ItemAckForwarder
import org.sbm4j.ktscraping.core.processors.ItemForwarder
import org.sbm4j.ktscraping.core.processors.RequestForwarder
import org.sbm4j.ktscraping.core.processors.ResponseForwarder
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventPropagation
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.GoogleSearchImageRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.stats.StatsCrawlerResult
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.channels.sendSyncAll
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.AbstractProcessingNode
import org.sbm4j.meercat.nodes.logger

/**
 * Base abstract class for the Engine — the central node of the KtScraping topology.
 *
 * The Engine sits at the junction of the three branches and is composed of three
 * internal sub-components, each subscribed to the [CrawlerChannelManager.spiderChannel]
 * via Meercat's SharedFlow mechanism:
 *
 * - [innerRequestForwarder]: forwards [AbstractRequest]s and [Response] backs between
 *   the Spider and Downloader branches. Also handles [EventPropagation.DOWNLOADER] events.
 * - [innerItemForwarder]: forwards [Item]s and [ItemAck] backs between the Spider and
 *   Pipeline branches. Also handles [EventPropagation.PIPELINE] events.
 * - [innerEventPropagator]: handles [EventPropagation.BOTH] events by broadcasting
 *   them to both the Downloader and Pipeline channels via [sendSyncAll], then
 *   reinjecting the aggregated back into the Spider channel.
 *
 * Concrete subclasses override the processing hooks ([processRequest], [processResponse],
 * [processItem], [processItemAck]) to observe and collect statistics on transiting messages,
 * and implement [computeResult] to return the final [CrawlerResult] at the end of the session.
 *
 * [pause] and [resume] are reserved for future lifecycle management.
 *
 * @param crawlerChannelManager The channel manager exposing the three topology channels.
 *
 * @see Engine
 * @see CrawlerChannelManager
 * @see CrawlerResult
 */
abstract class AbstractEngine(
    val crawlerChannelManager: CrawlerChannelManager,
) : AbstractComponent(){

    override val name: String = "Engine"

    /**
     * Hook called when an [AbstractRequest] transits through the Engine toward the Downloader.
     * Override to observe or collect statistics on outgoing requests.
     */
    open suspend fun processRequest(request: AbstractRequest){}

    /**
     * Hook called when a [Response] transits through the Engine back toward the Spider.
     * Override to observe or collect statistics on incoming responses.
     */
    open suspend fun processResponse(response: Response){}

    /**
     * Hook called when an [Item] transits through the Engine toward the Pipeline.
     * Override to observe or collect statistics on outgoing items.
     */
    open suspend fun processItem(item: Item){}

    /**
     * Hook called when an [ItemAck] transits through the Engine back toward the Spider.
     * Override to observe or collect statistics on item acknowledgements.
     */

    open suspend fun processItemAck(itemAck: ItemAck){}

    /**
     * Internal sub-component forwarding [AbstractRequest]s and [Response] backs
     * between the Spider and Downloader branches.
     *
     * Subscribes to [CrawlerChannelManager.spiderChannel] and forwards to
     * [CrawlerChannelManager.downloaderChannel]. Also handles [EventPropagation.DOWNLOADER]
     * events and their backs.
     */
    val innerRequestForwarder: AbstractProcessingNode = object :
        RequestForwarder,
        ResponseForwarder,
        AbstractMiddleComponent("${this@AbstractEngine.name}-RequestForwarder")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.crawlerChannelManager.spiderChannel
            set(value) { }

        override var outChannel: SuperChannel
            get() = this@AbstractEngine.crawlerChannelManager.downloaderChannel
            set(value) {}

        override var scope: CoroutineScope
            get() = this@AbstractEngine.scope
            set(value) {}


        override suspend fun processRequest(request: AbstractRequest): Any? {
            this@AbstractEngine.processRequest(request)
            return true
        }


        override suspend fun processResponse(response: Response) {
            this@AbstractEngine.processResponse(response)
        }


        override suspend fun run() {
            super<RequestForwarder>.run()
            super<ResponseForwarder>.run()

            registerEventListening(EventPropagation.DOWNLOADER)
            registerEventBackListening(EventPropagation.DOWNLOADER)
        }
    }

    /**
     * Internal sub-component forwarding [Item]s and [ItemAck] backs
     * between the Spider and Pipeline branches.
     *
     * Subscribes to [CrawlerChannelManager.spiderChannel] and forwards to
     * [CrawlerChannelManager.pipelineChannel]. Also handles [EventPropagation.PIPELINE]
     * events and their backs.
     */
    val innerItemForwarder: AbstractMiddleComponent = object:
        ItemForwarder,
        ItemAckForwarder,
        AbstractMiddleComponent("${this@AbstractEngine.name}-ItemForwarder")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.crawlerChannelManager.spiderChannel
            set(value) { }

        override var outChannel: SuperChannel
            get() = this@AbstractEngine.crawlerChannelManager.pipelineChannel
            set(value) {}

        override var scope: CoroutineScope
            get() = this@AbstractEngine.scope
            set(value) {}


        override suspend fun processItem(item: Item): Any? {
            this@AbstractEngine.processItem(item)
            return super.processItem(item)
        }

        override suspend fun processItemAck(itemAck: ItemAck) {
            this@AbstractEngine.processItemAck(itemAck)
            super.processItemAck(itemAck)
        }

        override suspend fun run() {
            super<ItemForwarder>.run()
            super<ItemAckForwarder>.run()

            registerEventListening(EventPropagation.PIPELINE)
            registerEventBackListening(EventPropagation.PIPELINE)
        }
    }

    /**
     * Internal sub-component handling [EventPropagation.BOTH] events.
     *
     * Subscribes to [CrawlerChannelManager.spiderChannel] and filters only events
     * with [EventPropagation.BOTH]. For each such event, broadcasts it to both
     * [CrawlerChannelManager.downloaderChannel] and [CrawlerChannelManager.pipelineChannel]
     * via [sendSyncAll], then reinjects the aggregated [EventBack] into the Spider channel.
     *
     * This sub-component has no [outChannel] — it sends directly to both downstream
     * channels and handles back aggregation internally.
     */
    val innerEventPropagator = object:
        AbstractMiddleComponent("${this@AbstractEngine.name}-EventPropagator")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.crawlerChannelManager.spiderChannel
            set(value) {}


        override var scope: CoroutineScope
            get() = this@AbstractEngine.scope
            set(value) {}

        override suspend fun sendPostProcess(send: Send, result: Any) {
            logger.debug{"$name: forward event to downloader and pipeline channel: $send"}
            val result = sendSyncAll(
                listOf(
                    this@AbstractEngine.crawlerChannelManager.downloaderChannel,
                    this@AbstractEngine.crawlerChannelManager.pipelineChannel
                ),
                send)
            logger.debug{"$name: result $result... send it back"}

            inChannel.send(result)
            logger.debug{"$name: sent it back"}
        }



        override suspend fun run() {
            val clazz = Event::class
            val flow = inChannel.getSendFlow(clazz).filter {
                it.propagation == EventPropagation.BOTH
            }
            this.performSends(clazz, flow, ::consumeEvent)
        }
    }


    /**
     * Computes and returns the final [CrawlerResult] at the end of the scraping session.
     *
     * Called by the [TopologyManager] after all Spiders have completed. Concrete
     * implementations aggregate the statistics collected via the processing hooks.
     *
     * @return The final result of the scraping session.
     */
    abstract fun computeResult(): CrawlerResult


    override suspend fun run() {
        logger.info { "${name}: starting engine" }

        innerRequestForwarder.run()
        innerItemForwarder.run()
        innerEventPropagator.run()

        this.crawlerChannelManager.spiderChannel.awaitReady()
        this.crawlerChannelManager.downloaderChannel.awaitReady()
        this.crawlerChannelManager.pipelineChannel.awaitReady()
    }

    override suspend fun stop() {
        logger.info { "${name}: stopping engine" }
        innerRequestForwarder.stop()
        innerItemForwarder.stop()
        innerEventPropagator.stop()
    }

    /** Reserved for future lifecycle management. */
    override suspend fun pause() {
        TODO("Not yet implemented")
    }
    /** Reserved for future lifecycle management. */
    override suspend fun resume() {
        TODO("Not yet implemented")
    }
}


/**
 * Default concrete implementation of [AbstractEngine].
 *
 * Collects scraping statistics in a [StatsCrawlerResult] and tracks item processing
 * progress via a [ProgressMonitor]. Statistics are updated for every transiting message:
 *
 * - [processRequest]: increments [StatsCrawlerResult.nbRequests] and, for
 *   [GoogleSearchImageRequest], [StatsCrawlerResult.nbGoogleAPIRequests].
 * - [processResponse]: increments [StatsCrawlerResult.responseOK] or
 *   [StatsCrawlerResult.responseError] based on the response status.
 * - [processItem]: increments [StatsCrawlerResult.nbItems] and categorizes the item
 *   as a new item ([ObjectDataItem]), an update ([ItemUpdate]), or a deletion ([ItemDelete]).
 * - [processItemAck]: increments [ProgressMonitor.receivedItemAck] to track
 *   how many items have been fully processed by the Pipeline.
 *
 * @param crawlerChannelManager The channel manager exposing the three topology channels.
 * @param progressMonitor The progress monitor feeding the UI with item processing progress.
 *
 * @see AbstractEngine
 * @see StatsCrawlerResult
 * @see ProgressMonitor
 */
class Engine(
    crawlerChannelManager: CrawlerChannelManager,
    val progressMonitor: ProgressMonitor
) : AbstractEngine(crawlerChannelManager){

    /** Statistics collected during the scraping session. */
    val stats: StatsCrawlerResult = StatsCrawlerResult()


    override suspend fun processRequest(request: AbstractRequest) {
        stats.nbRequests++
        if(request is GoogleSearchImageRequest){
            stats.nbGoogleAPIRequests++
        }
    }

    override suspend fun processResponse(response: Response) {
        when(response.status){
            Status.OK -> stats.responseOK++
            else -> stats.responseError++
        }
    }

    override suspend fun processItem(item: Item) {
        when(item){
            is ItemUpdate -> {
                this.stats.nbItems++
                this.stats.incrUpdate(item.label)
            }
            is ItemDelete -> {
                this.stats.nbItems++
                this.stats.incrDelete(item.label)
            }
            is DataItem<*> -> {
                this.stats.nbItems++
                if(item is ObjectDataItem<*>) {
                    this.stats.incrNew(item.label)
                }
            }
            else -> {}//super.performItem(item)
        }
    }

    override suspend fun processItemAck(itemAck: ItemAck) {
        progressMonitor.receivedItemAck++
    }


    override fun computeResult(): CrawlerResult {
        return stats
    }

}