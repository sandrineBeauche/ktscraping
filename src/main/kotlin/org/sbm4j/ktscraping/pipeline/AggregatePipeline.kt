package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.EventPropagation
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.SendException
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class AggregateEvent(
    override var sender: SendSource
): Event(sender, "aggregate", EventPropagation.PIPELINE){
    override fun clone(): Event {
        return this.copy()
    }
}

abstract class AggregatePipeline(name: String): AbstractPipeline(name) {

    abstract fun accumulateItem(item: Item)

    abstract fun aggregate(): List<Item>

    override suspend fun processItem(item: Item): ItemAck {
        try {
            accumulateItem(item)
            return item.buildBack()
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            val back = item.buildErrorBack(error)
            return back
        }
    }

    override suspend fun preCustomEvent(event: Event): Any? {
        return when(event){
            is AggregateEvent -> preAggregateEvent(event)
            else-> null
        }
    }

    override suspend fun postCustomEvent(event: EventBack) {
        when(event.send){
            is AggregateEvent -> postAggregateEvent(event)
            else-> throw IllegalArgumentException("event type not supported!")
        }
    }

    suspend fun preAggregateEvent(aggregate: AggregateEvent): Any?{
        val items = aggregate() as MutableList
        try{
            val back = sendSyncAggregate(items)
            return aggregate.buildBack()
        }
        catch (ex: SendException){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            return aggregate.buildErrorBack(error)
        }
    }



    suspend fun postAggregateEvent(aggregate: EventBack){

    }

}