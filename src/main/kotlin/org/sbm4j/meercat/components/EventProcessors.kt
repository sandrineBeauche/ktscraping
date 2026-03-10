package org.sbm4j.meercat.components

import kotlinx.coroutines.Deferred
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.meercat.channels.Status
import java.util.concurrent.ConcurrentHashMap


typealias EventJobResult = Deferred<Pair<Status, List<ErrorInfo>>>


interface EventProcessor{
    suspend fun performEvent(event: Event): EventJobResult?{
        return when(event){
            is StartEvent -> preStart(event)
            is EndEvent -> preEnd(event)
            else -> preCustomEvent(event)
        }
    }

    suspend fun preStart(event: Event): EventJobResult?{
        return null
    }

    suspend fun preEnd(event: Event): EventJobResult?{
        return null
    }

    suspend fun preCustomEvent(event: Event): EventJobResult?{
        return null
    }

    suspend fun performPostEvent(event: EventBack){
        when(event.send.eventName){
            "start" -> postStart(event)
            "end" -> postEnd(event)
            else -> postCustomEvent(event)
        }
    }

    suspend fun postStart(event: EventBack){}

    suspend fun postEnd(event: EventBack){}

    suspend fun postCustomEvent(event: EventBack){}
}

interface EventConsumer: SendConsumer, EventProcessor {

    val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>


    suspend fun consumeEvent(event: Event): Any?{
        logger.trace{"${name}: received an event to consume: ${event}"}
        val eventJob = performEvent(event)
        if(eventJob != null) {
            pendingEventJobs[event.eventName] = eventJob
        }
        return event
    }


    override suspend fun run() {
        val clazz = Event::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::consumeEvent)
    }
}

interface EventBackForwarder: BackForwarder, EventProcessor {

    val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>

    suspend fun resumeEvent(event: EventBack){
        try {
            val eventName = event.send.eventName
            val job = pendingEventJobs.remove(eventName)
            val result = job?.await()
            if(job != null && result != null) {
                event.status += result.first
                event.errorInfos.addAll(result.second)
            }
            performPostEvent(event)
        }
        catch(ex: Exception){
            val infos = generateErrorInfos(ex)
            event.errorInfos.add(infos)
        }
    }


    override suspend fun run() {
        val flow = this@EventBackForwarder.outChannel.getBackFlow(EventBack::class, this)
        receiveBacks(EventBack::class, flow, ::resumeEvent)
    }
}


interface EventSink: EventConsumer{

    override suspend fun consumeEvent(event: Event): Any? {
        lateinit var result: EventBack
        try {
            performEvent(event)
            result = event.buildBack()
        }
        catch(ex: Exception){
            val error = this.generateErrorInfos(ex)
            result = event.buildErrorBack(error)
        }
        finally {
            performPostEvent(result)
        }
        return result
    }
}