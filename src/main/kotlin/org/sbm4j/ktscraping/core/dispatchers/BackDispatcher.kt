package org.sbm4j.ktscraping.core.dispatchers

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.Controllable
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.logger
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface BackDispatcher: Controllable, DIAware {

    val channelsIns : MutableList<SuperChannel>

    val channelOut: SuperChannel

    val pendingAnswerable: MutableMap<UUID, SuperChannel>

    val pendingEvent: ConcurrentHashMap<String, MutableList<Event>>

    suspend fun performSends(){
        for ((index, channel) in channelsIns.withIndex()) {
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
                logger.debug { "$name: finished receiving sends" }
            }
        }
    }

    suspend fun performEvent(request: Event, sender: SuperChannel, index: Int){
        val event = request.eventName
        logger.debug { "${name}: received event with name $event from input #$index"}
        val events = pendingEvent.getOrPut(event) { mutableListOf() }
        events.add(request)
        pendingAnswerable[request.channelableId] = sender
        if(events.size >= channelsIns.size){
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
                    is EventBack -> performEventBack(back)
                    else -> performAnswerableBack(back)
                }
            }
            logger.debug { "$name: finished receiving backs" }
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
        for(sender in channelsIns){
            sender.close()
        }
        super.stop()
    }
}

class SpiderDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    override val di: DI
): BackDispatcher, AbstractControllable(){

    override val channelsIns: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelOut: SuperChannel

    override val pendingAnswerable: MutableMap<UUID, SuperChannel> = ConcurrentHashMap()

    override val pendingEvent: ConcurrentHashMap<String, MutableList<Event>> = ConcurrentHashMap()


    fun addBranch(channel: SuperChannel){
        this.channelsIns.add(channel)
    }
}