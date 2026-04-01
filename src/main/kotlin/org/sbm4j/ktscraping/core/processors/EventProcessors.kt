package org.sbm4j.ktscraping.core.processors

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.filter
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.EventPropagation
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.data.ErrorInfo

import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer

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

    suspend fun registerEventListening(propagation: EventPropagation? = null){
        val clazz = Event::class
        val flow = inChannel.getSendFlow(clazz)
        val filtered = if(propagation != null)
            flow.filter({ it.propagation == propagation })
        else flow
        this.performSends(clazz, filtered, ::consumeEvent)
    }


    override suspend fun run() {
        registerEventListening()
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

    suspend fun registerEventBackListening(propagation: EventPropagation? = null){
        val flow = this@EventBackForwarder.outChannel.getBackFlow(EventBack::class, this)
        val filtered = if(propagation != null)
            flow.filter { it.send.propagation == propagation }
        else flow
        receiveBacks(EventBack::class, filtered, ::resumeEvent)
    }

    override suspend fun run() {
        registerEventBackListening()
    }
}


interface EventSink: EventConsumer{

    override suspend fun consumeEvent(event: Event): Any? {
        lateinit var result: EventBack
        try {
            performEvent(event)?.join()
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