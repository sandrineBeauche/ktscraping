package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.GoogleSearchImageRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.stats.StatsCrawlerResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap



abstract class AbstractEngine(
    channelFactory: ChannelFactory,
) : RequestSender, RequestReceiver, ItemSecureForwarder{

    override val mutex: Mutex = Mutex()
    override var state = State()

    override val name: String = "Engine"

    override var itemIn: ReceiveChannel<Item> = channelFactory.spiderItemChannel
    override var itemOut: SendChannel<Item> = channelFactory.itemChannel

    override var itemAckIn: ReceiveChannel<AbstractItemAck> = channelFactory.itemAckChannel

    override var requestIn: ReceiveChannel<AbstractRequest> = channelFactory.spiderRequestChannel
    override val responseOut: SendChannel<Response<*>> = channelFactory.spiderResponseChannel

    override val requestOut: SendChannel<AbstractRequest> = channelFactory.downloaderRequestChannel
    override val responseIn: ReceiveChannel<Response<*>> = channelFactory.downloaderResponseChannel

    override var pendingRequests: PendingRequestMap = PendingRequestMap()



    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override val pendingMinorError: ConcurrentHashMap<Int, MutableList<ErrorInfo>> = ConcurrentHashMap()

    val pendingEventItems: ConcurrentHashMap<UUID, Channel<EventItemAck>> = ConcurrentHashMap()



    override lateinit var scope: CoroutineScope

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        this.requestOut.send(request)
    }

    override suspend fun performEvent(event: Event): EventJobResult? {
        when(event){
            is EventRequest -> {
                if(event.generateItem){
                    val eventItem = event.generateEventItem()
                    return sendEventItem(eventItem)
                }
                else return null
            }
            is EventItem -> {
                logger.trace{ "$name: forwards event item: $event"}
                itemOut.send(event)
                return null
            }
            else -> return null
        }
    }


    fun sendEventItem(event: EventItem): EventJobResult{
        val respChannel = Channel<EventItemAck>(Channel.RENDEZVOUS)
        pendingEventItems[event.itemId] = respChannel
        val job = this.scope.async(CoroutineName("${name}-event-${event.eventName}")){
            logger.trace{ "$name: send item from event request and waits for a response"}
            itemOut.send(event)
            val itemAck = respChannel.receive() as EventItemAck
            logger.trace { "$name: received item ack for ${itemAck.eventName} event item" }
            return@async Pair(itemAck.status, itemAck.errorInfos)
        }
        return job
    }

    override suspend fun resumeEvent(event: EventBack) {
        logger.trace{ "$name: received event back: $event" }
        when(event){
            is EventResponse -> {
                super<ItemSecureForwarder>.resumeEvent(event)
                responseOut.send(event)
            }
            is EventItemAck -> {
                val respChannel = pendingEventItems.remove(event.itemId)
                logger.trace { "$name: dispatches item event ack to the corresponding coroutine: $event" }
                respChannel?.send(event)
            }
        }



    }


    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        return listOf(item)
    }


    abstract fun computeResult(): CrawlerResult


    override suspend fun performAck(itemAck: AbstractItemAck) {
        pendingEventItems.remove(itemAck.itemId)

    }



    override suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        this.responseOut.send(response)
    }


    override suspend fun run() {
        logger.info { "${name}: starting engine" }
        super<RequestSender>.run()
        super<RequestReceiver>.run()
        super<ItemSecureForwarder>.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: stopping engine" }
        super<RequestSender>.stop()
        super<RequestReceiver>.stop()
        super<ItemSecureForwarder>.stop()
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

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        stats.nbRequests++
        if(request is GoogleSearchImageRequest){
            stats.nbGoogleAPIRequests++
        }
        return true
    }



    override fun computeResult(): CrawlerResult {
        return stats
    }

    override suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        when(response.status){
            Status.OK -> stats.responseOK++
            else -> stats.responseError++
        }
        super.performDownloadingResponse(response, request)
    }

    override suspend fun performItem(item: Item) {
        when(item){
            is ErrorItem -> stats.errors.add(item)
            is ProgressItem -> progressMonitor.processItemProgress(item)
            is ItemUpdate -> {
                this.stats.nbItems++
                this.stats.incrUpdate(item.label)
            }
            is ItemDelete -> {
                this.stats.nbItems++
                this.stats.incrDelete(item.label)
            }
            else -> super.performItem(item)
        }
    }

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        this.stats.nbItems++
        if(item is ObjectDataItem<*>) {
            this.stats.incrNew(item.label)
        }
        return super.processDataItem(item)
    }


    override suspend fun performAck(itemAck: AbstractItemAck) {
        progressMonitor.receivedItemAck++
        super.performAck(itemAck)
    }
}