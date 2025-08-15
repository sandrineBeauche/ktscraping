package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.GoogleSearchImageRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.stats.StatsCrawlerResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


abstract class AbstractEngine(
    channelFactory: ChannelFactory,
) : SendConsumer<Send>, SendSource<Send, Back<Send>>, EventConsumer{

    override val mutex: Mutex = Mutex()
    override var state = State()

    override val name: String = "Engine"

    override lateinit var inChannel: SuperChannel

    override lateinit var outChannel: SuperChannel

    lateinit var pipelineChannel: SuperChannel

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override val pendingMinorError: ConcurrentHashMap<UUID, MutableList<ErrorInfo>> = ConcurrentHashMap()

    val pendingEventItems: ConcurrentHashMap<UUID, Channel<EventItemAck>> = ConcurrentHashMap()



    override lateinit var scope: CoroutineScope

    val semaphore: Semaphore = Semaphore(1, 1)

    suspend fun waitStarted() {
        semaphore.acquire()
    }

    override suspend fun sendPostProcess(send: Send, result: Any) {
        this.outChannel.send(send)
    }


    override suspend fun performEvent(event: Event): EventJobResult? {
        when(event){
            is EventRequest -> {
                if(event is StartRequest){
                    semaphore.release()
                }
                if(event.generateItem){
                    val eventItem = event.generateEventItem()
                    return sendEventItem(eventItem)
                }
                else return null
            }
            is EventItem -> {
                logger.trace{ "$name: forwards event item: $event"}
                pipelineChannel.send(event)
                return null
            }
            else -> return null
        }
    }


    fun sendEventItem(event: EventItem): EventJobResult{
        val respChannel = Channel<EventItemAck>(Channel.RENDEZVOUS)
        pendingEventItems[event.channelableId] = respChannel
        val job = this.scope.async(CoroutineName("${name}-event-${event.eventName}")){
            logger.trace{ "$name: send item from event request and waits for a response"}
            pipelineChannel.send(event)
            val itemAck = respChannel.receive() as EventItemAck
            logger.trace { "$name: received item ack for ${itemAck.send.eventName} event item" }
            return@async Pair(itemAck.status, itemAck.errorInfos)
        }
        return job
    }



    override suspend fun resumeEvent(event: EventBack<*>) {
        logger.trace{ "$name: received event back: $event" }
        when(event){
            is EventResponse -> {
                super.resumeEvent(event)
                outChannel.send(event)
            }
            is EventItemAck -> {
                val respChannel = pendingEventItems.remove(event.channelableId)
                logger.trace { "$name: dispatches item event ack to the corresponding coroutine: $event" }
                respChannel?.send(event)
            }
        }
    }




    open suspend fun processDataItem(item: DataItem<*>): List<Item> {
        return listOf(item)
    }


    abstract fun computeResult(): CrawlerResult



    open suspend fun performDataAck(itemAck: DataItemAck) {
        pendingEventItems.remove(itemAck.channelableId)

    }



    open suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        this.outChannel.send(response)
    }


    override suspend fun run() {
        logger.info { "${name}: starting engine" }

    }

    override suspend fun stop() {
        logger.info { "${name}: stopping engine" }
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



    suspend fun processDataRequest(request: DownloadingRequest): Any? {
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

    suspend fun performItem(item: Item) {
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
            else -> {}//super.performItem(item)
        }
    }

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        this.stats.nbItems++
        if(item is ObjectDataItem<*>) {
            this.stats.incrNew(item.label)
        }
        return super.processDataItem(item)
    }


    override suspend fun performDataAck(itemAck: DataItemAck) {
        progressMonitor.receivedItemAck++
        super.performDataAck(itemAck)
    }

    override suspend fun processSend(send: Send): Any? {
        TODO("Not yet implemented")
    }

    override val sendClazz: KClass<*>
        get() = TODO("Not yet implemented")

    override suspend fun performBack(back: Back<Send>) {
        TODO("Not yet implemented")
    }

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        TODO("Not yet implemented")
    }
}