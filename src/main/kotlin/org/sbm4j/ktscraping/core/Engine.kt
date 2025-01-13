package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.*
import java.util.UUID

abstract class AbstractEngine(
    final override val scope: CoroutineScope,
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

    override fun processItem(item: Item): List<Item> {
        return if(item is ItemEnd){
            receivedItemEnd = true
            listOf()
        } else{
            pendingItems.add(item.itemId)
            listOf(item)
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
                logger.debug{ "${name}: Received an item ack to process" }
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

    override suspend fun start() {
        logger.info { "${name}: starting engine" }
        super<RequestSender>.start()
        super<RequestReceiver>.start()
        super<ItemFollower>.start()
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


data class StatsCrawlerResult(val nbRequests: Int, val nbItems: Int): CrawlerResult

class Engine(
    scope: CoroutineScope,
    channelFactory: ChannelFactory,
    val progressMonitor: ProgressMonitor
) : AbstractEngine(scope, channelFactory){

    override suspend fun processRequest(request: AbstractRequest): Any? {
        progressMonitor.receivedRequest++
        return true
    }

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {

        this.requestOut.send(request)
    }

    override fun computeResult(): CrawlerResult {
        return StatsCrawlerResult(this.progressMonitor.receivedResponse, this.progressMonitor.receivedItem)
    }

    override suspend fun performResponse(response: Response) {
        progressMonitor.receivedResponse++
        super.performResponse(response)
    }

    override fun processItem(item: Item): List<Item> {
        if(item !is ItemEnd) {
            progressMonitor.receivedItem++
        }
        return super.processItem(item)
    }


    override suspend fun performAck(itemAck: ItemAck) {
        progressMonitor.receivedItemAck++
        super.performAck(itemAck)
    }
}