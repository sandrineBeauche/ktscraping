package org.sbm4j.ktscraping.core.dispatchers

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.core.components.AbstractControllable
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface BackDispatcher: Controllable, DIAware {

    val senders : MutableList<SuperChannel>

    val channelOut: SuperChannel

    val pendingAnswerable: MutableMap<UUID, SuperChannel>

    val pendingEvent: ConcurrentHashMap<String, MutableList<Event>>

    suspend fun performSends(){
        for ((index, channel) in senders.withIndex()) {
            scope.launch(CoroutineName("${name}-performSends-${index}")) {
                channel.getSendFlow().collect { send ->
                    when(send){
                        is Event -> performEvent(send, channel, index)
                        is DownloadingRequest, is Item -> performAnswerable(send, channel, index)
                        else -> {
                            channelOut.send(send)
                        }
                    }
                }
            }
        }
    }

    suspend fun performEvent(request: Event, sender: SuperChannel, index: Int){
        val event = request.eventName
        logger.debug { "${name}: received event with name $event from input #$index"}
        val events = pendingEvent.getOrPut(event) { mutableListOf() }
        events.add(request)
        pendingAnswerable[request.channelableId] = sender
        if(events.size >= senders.size){
            channelOut.send(request)
        }
    }

    suspend fun performAnswerable(send: Send, sender: SuperChannel, index: Int){
        logger.trace { "Received send ${send.name} from input #$index and forwards it" }
        pendingAnswerable[send.channelableId] = sender
        channelOut.send(send)
    }


    suspend fun performBacks(){
        scope.launch(CoroutineName("${name}-performBacks")) {
            channelOut.getBackFlow().collect { back ->
                when (back) {
                    is DownloadingResponse, is Item -> performAnswerableBack(back)
                    is EventBack -> performEventBack(back)
                }
            }
        }
    }

    suspend fun performAnswerableBack(back: Back<*>){
        logger.trace{ "Received back for the send ${back.send.name} and dispatch it"}
        val channel = pendingAnswerable.remove(back.send.channelableId)
        channel?.send(back)
    }

    suspend fun performEventBack(back: EventBack){
        logger.debug { "${name}: received an event ${back.send.eventName}, forwards it to all senders" }
        val requests = pendingEvent.remove(back.send.eventName)
        if(requests != null){
            for(req in requests){
                val resp = back.copy(send = req)
                val channel = pendingAnswerable.remove(req.channelableId)
                channel?.send(resp)
            }

        }
    }


    override suspend fun run() {
        logger.info{ "Starting the back dispatcher ${name}"}
        this.performSends()
        this.performBacks()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the back dispatcher ${name}"}
        this.channelOut.close()
        for(sender in senders){
            sender.close()
        }
    }
}

class SpiderDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    override val di: DI
): BackDispatcher, AbstractControllable(){

    override val senders: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelOut: SuperChannel

    override val pendingAnswerable: MutableMap<UUID, SuperChannel> = ConcurrentHashMap()

    override val pendingEvent: ConcurrentHashMap<String, MutableList<Event>> = ConcurrentHashMap()


    fun addBranch(channel: SuperChannel){
        this.senders.add(channel)
    }
}