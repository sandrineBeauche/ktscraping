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
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import java.util.concurrent.ConcurrentHashMap

interface RequestDispatcher: Controllable, DIAware {

    val senders: MutableList<SendChannel<AbstractRequest>>

    val receivers: MutableList<ReceiveChannel<Response<*>>>

    val channelOut: SendChannel<Response<*>>

    val channelIn: ReceiveChannel<AbstractRequest>

    val pendingRequestEvent: ConcurrentHashMap<String, MutableList<EventResponse>>

    suspend fun performRequests(){
        scope.launch(CoroutineName("${name}-performRequests")){
            for(request in channelIn){
                when(request){
                    is EventRequest -> performEventRequest(request)
                    is DownloadingRequest -> performDownloadingRequest(request)
                }
            }
        }
    }

    suspend fun performEventRequest(event: EventRequest){
        pendingRequestEvent[event.eventName] = mutableListOf()
        logger.trace { "${name}: Received event request ${event.name} and dispatch it to all" }
        senders.forEach {
            it.send(event.clone())
        }
    }

    suspend fun performDownloadingRequest(request: DownloadingRequest){
        val channel = selectChannel(request)
        logger.trace { "${name}: Received request ${request.name} and dispatch it" }
        channel.send(request)
    }

    fun selectChannel(request: DownloadingRequest): SendChannel<AbstractRequest>

    suspend fun performResponses(){
        for((index, receiver) in receivers.withIndex()) {
            scope.launch(CoroutineName("${name}-performResponses-${index}")) {
                for (response in receiver) {
                    logger.trace { "${name}: Received response for the request ${response.send.name} and forwards it" }
                    when(response){
                        is EventResponse -> performEventResponse(response, response.send)
                        is DownloadingResponse -> channelOut.send(response)
                    }
                }
            }
        }
    }

    suspend fun performEventResponse(response: EventResponse, event: EventRequest){
        val name = event.eventName
        val responses = pendingRequestEvent.getOrPut(name) { mutableListOf() }
        responses.add(response)
        if(responses.size >= senders.size){
            pendingRequestEvent.remove(name)
            val newResp = responses.reduce{ acc, response ->
                EventResponse(acc.send,
                    acc.status + response.status,
                    (acc.errorInfos + response.errorInfos) as MutableList<ErrorInfo>
                ) }
            channelOut.send(newResp)
        }
    }

    override suspend fun run() {
        logger.info{ "Starting the request dispatcher ${name}"}
        performRequests()
        performResponses()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the response dispatcher ${name}"}
        this.channelOut.close()
        for(sender in senders){
            sender.close()
        }
    }
}

abstract class DownloaderRequestDispatcher(
    override val name: String = "DownloaderRequestDispatcher",
    override val di: DI
): RequestDispatcher{

    override val senders: MutableList<SendChannel<AbstractRequest>> = mutableListOf()
    override val receivers: MutableList<ReceiveChannel<Response<*>>> = mutableListOf()

    override lateinit var channelOut: SendChannel<Response<*>>
    override lateinit var channelIn: ReceiveChannel<AbstractRequest>

    override val pendingRequestEvent: ConcurrentHashMap<String, MutableList<EventResponse>> = ConcurrentHashMap()

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var scope: CoroutineScope

    fun addBranch(reqChannel: Channel<AbstractRequest>, respChannel: Channel<Response<*>>){
        this.senders.add(reqChannel)
        this.receivers.add(respChannel)
    }
}