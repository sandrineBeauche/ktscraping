package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.requests.*
import org.sbm4j.ktscraping.stats.StatsCrawlerResult
import java.util.*

abstract class AbstractEngine(
    channelFactory: ChannelFactory,
) : RequestSender, RequestReceiver, ItemFollower{

    override val mutex: Mutex = Mutex()
    override var state = State()

    override val name: String = "Engine"

    override var itemIn: ReceiveChannel<Item> = channelFactory.spiderItemChannel
    override var itemOut: SendChannel<Item> = channelFactory.itemChannel

    var itemAckIn: ReceiveChannel<ItemAck> = channelFactory.itemAckChannel

    override var requestIn: ReceiveChannel<AbstractRequest> = channelFactory.spiderRequestChannel
    override val responseOut: SendChannel<Response> = channelFactory.spiderResponseChannel

    override val requestOut: SendChannel<AbstractRequest> = channelFactory.downloaderRequestChannel
    override val responseIn: ReceiveChannel<Response> = channelFactory.downloaderResponseChannel

    override var pendingRequests: PendingRequestMap = PendingRequestMap()

    var receivedItemEnd: Boolean = false

    val pendingItems: MutableList<UUID> = mutableListOf()

    val resultChannel: SendChannel<CrawlerResult> = Channel<CrawlerResult>(Channel.RENDEZVOUS)

    override lateinit var scope: CoroutineScope

    override suspend fun processItem(item: Item): List<Item> {
        return when(item){
            is ItemEnd -> {
                receivedItemEnd = true
                listOf()
            }
            is ItemProgress -> {
                listOf()
            }
            is ItemError -> {
                listOf()
            }
            else -> {
                listOf(item)
            }
        }
    }

    abstract fun computeResult(): CrawlerResult


    override suspend fun performAck(itemAck: ItemAck) {
        pendingItems.remove(itemAck.itemId)
        if(receivedItemEnd && pendingItems.isEmpty()){
            val result = computeResult()
            resultChannel.send(result)
        }
    }

    override suspend fun performAcks(){
        scope.launch(CoroutineName("${name}-performAcks")){
            logger.debug { "${name}: Waiting for items acks to follow" }
            for(itemAck in itemAckIn){
                logger.trace{ "${name}: Received an item ack to process" }
                performAck(itemAck)
            }
        }
    }

    override suspend fun performItems() {
        super.performItems()
    }

    override suspend fun performResponse(response: Response) {
        this.responseOut.send(response)
    }


    override suspend fun run() {
        logger.info { "${name}: starting engine" }
        super<RequestSender>.run()
        super<RequestReceiver>.run()
        super<ItemFollower>.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: stopping engine" }
        super<RequestSender>.stop()
        super<RequestReceiver>.stop()
        super<ItemFollower>.stop()
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun resume() {
        TODO("Not yet implemented")
    }
}



class Engine(
    channelFactory: ChannelFactory,
    val progressMonitor: ProgressMonitor
) : AbstractEngine(channelFactory){

    val stats: StatsCrawlerResult = StatsCrawlerResult()

    override suspend fun processRequest(request: AbstractRequest): Any? {
        stats.nbRequests++
        if(request is GoogleSearchImageRequest){
            stats.nbGoogleAPIRequests++
        }
        return true
    }

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        this.requestOut.send(request)
    }

    override fun computeResult(): CrawlerResult {
        return stats
    }

    override suspend fun performResponse(response: Response) {
        when(response.status){
            Status.OK -> stats.responseOK++
            else -> stats.responseError++
        }
        super.performResponse(response)
    }

    override suspend fun processItem(item: Item): List<Item> {
        when(item){
            is ItemEnd -> {}
            is ItemError -> {
                stats.errors.add(item)
            }
            is ItemProgress -> {
                progressMonitor.processItemProgress(item)
            }
            is DataItem -> {
                this.stats.nbItems++
                this.stats.incrNew(item.label)
            }
            is ItemUpdate -> {
                this.stats.nbItems++
                this.stats.incrUpdate(item.label)
            }
            is ItemDelete -> {
                this.stats.nbItems++
                this.stats.incrDelete(item.label)
            }
        }
        return super.processItem(item)
    }


    override suspend fun performAck(itemAck: ItemAck) {
        progressMonitor.receivedItemAck++
        super.performAck(itemAck)
    }
}