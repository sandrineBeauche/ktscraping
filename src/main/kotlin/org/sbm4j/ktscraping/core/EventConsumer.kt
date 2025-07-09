package org.sbm4j.ktscraping.core

import kotlinx.coroutines.Job
import org.sbm4j.ktscraping.data.EndEvent
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.StartEvent
import org.sbm4j.ktscraping.data.response.EventResponse
import java.util.concurrent.ConcurrentHashMap

interface EventConsumer {

    val pendingEvent: ConcurrentHashMap<String, Job>


    suspend fun consumeEvent(event: Event): Any?{
        val eventJob = performEvent(event)
        if(eventJob != null) {
            pendingEvent[event.eventName] = eventJob
        }
        return event
    }

    suspend fun performEvent(event: Event): Job?{
        return when(event){
            is StartEvent -> preStart(event)
            is EndEvent -> preEnd(event)
            else -> preCustomEvent(event)
        }
    }

    suspend fun preStart(event: Event): Job?{
        return null
    }

    suspend fun preEnd(event: Event): Job?{
        return null
    }

    suspend fun preCustomEvent(event: Event): Job?{
        return null
    }

    suspend fun resumeEvent(event: EventResponse){
        val reqId = event.request.eventName
        val job = pendingEvent.remove(reqId)
        job?.join()
        performPostEvent(event)
    }

    suspend fun performPostEvent(event: EventResponse){
        when(event.request){
            is StartEvent -> postStart(event)
            is EndEvent -> postEnd(event)
            else -> postCustomEvent(event)
        }
    }

    suspend fun postStart(event: EventResponse){}

    suspend fun postEnd(event: EventResponse){}

    suspend fun postCustomEvent(event: EventResponse){}

}