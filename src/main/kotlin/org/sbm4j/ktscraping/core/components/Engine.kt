package org.sbm4j.ktscraping.core.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Semaphore
import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.core.ProgressMonitor
import org.sbm4j.ktscraping.core.channels.CrawlerChannelManager
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.channels.sendSyncAll
import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.ItemAckForwarder
import org.sbm4j.ktscraping.core.processors.ItemForwarder
import org.sbm4j.ktscraping.core.processors.RequestForwarder
import org.sbm4j.ktscraping.core.processors.ResponseForwarder
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
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
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.AbstractProcessingNode
import org.sbm4j.meercat.nodes.logger


abstract class AbstractEngine(
    val crawlerChannelManager: CrawlerChannelManager,
) : AbstractComponent(){

    override val name: String = "Engine"

    lateinit var spiderChannel: SuperChannel

    lateinit var downloaderChannel: SuperChannel

    lateinit var pipelineChannel: SuperChannel


    open suspend fun processRequest(request: AbstractRequest){}

    open suspend fun processResponse(response: Response){}

    open suspend fun processItem(item: Item){}

    open suspend fun processItemAck(itemAck: ItemAck){}

    val innerRequestForwarder: AbstractProcessingNode = object :
        RequestForwarder,
        ResponseForwarder,
        AbstractMiddleComponent("${this@AbstractEngine.name}-RequestForwarder")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.spiderChannel
            set(value) { }

        override var outChannel: SuperChannel
            get() = this@AbstractEngine.downloaderChannel
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


    val innerItemForwarder: AbstractMiddleComponent = object:
        ItemForwarder,
        ItemAckForwarder,
        AbstractMiddleComponent("${this@AbstractEngine.name}-ItemForwarder")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.spiderChannel
            set(value) { }

        override var outChannel: SuperChannel
            get() = this@AbstractEngine.pipelineChannel
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

    val innerEventPropagator = object:
        AbstractMiddleComponent("${this@AbstractEngine.name}-EventPropagator")
    {
        override var inChannel: SuperChannel
            get() = this@AbstractEngine.spiderChannel
            set(value) {}


        override var scope: CoroutineScope
            get() = this@AbstractEngine.scope
            set(value) {}

        override suspend fun sendPostProcess(send: Send, result: Any) {
            logger.debug{"$name: forward event to downloader and pipeline channel: $send"}
            val result = sendSyncAll(listOf(downloaderChannel, pipelineChannel), send)
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

    val semaphore: Semaphore = Semaphore(1, 1)

    suspend fun waitStarted() {
        semaphore.acquire()
    }



    abstract fun computeResult(): CrawlerResult


    override suspend fun run() {
        logger.info { "${name}: starting engine" }
        spiderChannel = crawlerChannelManager.spiderChannel
        downloaderChannel = crawlerChannelManager.downloaderChannel
        pipelineChannel = crawlerChannelManager.pipelineChannel

        innerRequestForwarder.run()
        innerItemForwarder.run()
        innerEventPropagator.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: stopping engine" }
        innerRequestForwarder.stop()
        innerItemForwarder.stop()
        innerEventPropagator.stop()
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun resume() {
        TODO("Not yet implemented")
    }
}



class Engine(
    crawlerChannelManager: CrawlerChannelManager,
    val progressMonitor: ProgressMonitor
) : AbstractEngine(crawlerChannelManager){

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