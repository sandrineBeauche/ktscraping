package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import java.util.concurrent.ConcurrentHashMap
import org.sbm4j.ktscraping.data.response.Response
import java.awt.event.ItemEvent
import kotlin.compareTo

interface ResponseDispatcher: Controllable, DIAware {

    val senders : MutableMap<ReceiveChannel<AbstractRequest>, SendChannel<Response<*>>>

    val channelOut: SendChannel<AbstractRequest>

    val channelIn: ReceiveChannel<Response<*>>

    val pendingRequests: PendingRequestMap


    val pendingRequestEvent: ConcurrentHashMap<String, MutableList<EventRequest>>

    suspend fun performRequests(){
        for ((index, entry) in senders.entries.withIndex()) {
            val (receiver, sender) = entry
            scope.launch(CoroutineName("${name}-performRequests-${index}")) {
                for(request in receiver) {
                    when(request){
                        is EventRequest -> performEventRequest(request, sender as Channel<Response<*>>, index)
                        is DownloadingRequest -> performDownloadingRequest(request, sender as Channel<Response<*>>, index)
                    }
                }
            }
        }
    }

    suspend fun performEventRequest(request: EventRequest, sender: Channel<Response<*>>, index: Int){
        val event = request.eventName
        logger.debug { "${name}: received event request with name $event from input #$index"}
        val events = pendingRequestEvent.getOrPut(event) { mutableListOf() }
        events.add(request)
        pendingRequests[request.channelableId] = sender
        if(events.size >= senders.size){
            channelOut.send(request)
        }
    }

    suspend fun performDownloadingRequest(request: AbstractRequest, sender: Channel<Response<*>>, index: Int){
        logger.trace { "Received request ${request.name} from input #$index and forwards it" }
        pendingRequests[request.channelableId] = sender
        channelOut.send(request)
    }


    suspend fun performResponses(){
        scope.launch(CoroutineName("${name}-performResponses")) {
            for(response in channelIn){
                when(response){
                    is DownloadingResponse -> performDownloadingResponse(response)
                    is EventResponse -> performEventResponse(response)
                }

            }
        }
    }

    suspend fun performDownloadingResponse(response: DownloadingResponse){
        logger.trace{ "Received response for the request ${response.send.name} and dispatch it"}
        val channel = pendingRequests.remove(response.send.channelableId)
        channel?.send(response)
    }

    suspend fun performEventResponse(response: EventResponse){
        logger.debug { "${name}: received an event ${response.eventName}, forwards it to all senders" }
        val requests = pendingRequestEvent.remove(response.eventName)
        if(requests != null){
            for(req in requests){
                val resp = response.copy(send = req)
                val channel = pendingRequests.remove(req.channelableId)
                channel?.send(resp)
            }

        }
    }


    override suspend fun run() {
        logger.info{ "Starting the response dispatcher ${name}"}
        this.performRequests()
        this.performResponses()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the response dispatcher ${name}"}
        this.channelOut.close()
        for(sender in senders.values){
            sender.close()
        }
    }
}

class SpiderResponseDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    override val di: DI
): ResponseDispatcher{

    override val senders: MutableMap<ReceiveChannel<AbstractRequest>, SendChannel<Response<*>>> = mutableMapOf()

    val itemSenders: MutableList<ReceiveChannel<Item>> = mutableListOf()

    override lateinit var channelOut: SendChannel<AbstractRequest>

    override lateinit var channelIn: ReceiveChannel<Response<*>>

    lateinit var itemChannelOut: SendChannel<Item>

    override val pendingRequests: PendingRequestMap = PendingRequestMap()
    override val pendingRequestEvent: ConcurrentHashMap<String, MutableList<EventRequest>> = ConcurrentHashMap()
    val pendingItemEvent: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

    override val mutex: Mutex = Mutex()

    override var state: State = State()

    override lateinit var scope: CoroutineScope

    fun performItems(){
        for((index, senderChannel) in itemSenders.withIndex()){
            scope.launch(CoroutineName("${name}-performItems-${index}")) {
                for(item in senderChannel){
                    when(item){
                        is EventItem -> performItemEvent(item, index)
                        is DataItem<*> -> {
                            logger.trace{ "${name}: Received an item from input #$index and forwards it: ${item}" }
                            itemChannelOut.send(item)
                        }
                    }
                }
            }
        }
    }

    suspend fun performItemEvent(item: EventItem, index: Int){
        logger.trace{ "${name}: Received an item from input #$index: ${item}..." }
        var nb = pendingItemEvent.getOrPut(item.eventName){0}
        nb++
        pendingItemEvent[item.eventName] = nb
        if(nb >= senders.size){
            pendingItemEvent.remove(item.eventName)
            logger.trace{ "... forward item $item"}
            itemChannelOut.send(item)
        }
    }


    override suspend fun run() {
        this.performItems()
        super.run()
    }

    override suspend fun stop() {
        this.itemChannelOut.close()
        super.stop()
    }

    fun addBranch(reqChannel: Channel<AbstractRequest>, respChannel: Channel<Response<*>>, itemChannel: Channel<Item>){
        this.senders.put(reqChannel, respChannel)
        this.itemSenders.add(itemChannel)
    }
}