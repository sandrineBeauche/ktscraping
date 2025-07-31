package org.sbm4j.ktscraping.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import java.util.concurrent.ConcurrentHashMap


typealias EventJobResult = Deferred<Pair<Status, List<ErrorInfo>>>

interface EventConsumer {

    val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>


    suspend fun consumeEvent(event: Event): Any?{
        val eventJob = performEvent(event)
        if(eventJob != null) {
            pendingEventJobs[event.eventName] = eventJob
        }
        return event
    }

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

    suspend fun resumeEvent(event: EventBack){
        try {
            val eventName = event.eventName
            val job = pendingEventJobs.remove(eventName)
            val result = job?.await()
            if(result != null) {
                event.status = event.status + result.first
                event.errorInfos.addAll(result.second)
            }
            performPostEvent(event)
        }
        catch(ex: Exception){
            val infos = generateErrorInfos(ex)
            event.errorInfos.add(infos)
        }
    }

    suspend fun performPostEvent(event: EventBack){
        when(event.eventName){
            "start" -> postStart(event)
            "end" -> postEnd(event)
            else -> postCustomEvent(event)
        }
    }

    suspend fun postStart(event: EventBack){}

    suspend fun postEnd(event: EventBack){}

    suspend fun postCustomEvent(event: EventBack){}

    fun generateErrorInfos(ex: Exception): ErrorInfo

}