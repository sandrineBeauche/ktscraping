package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.meercat.components.EventJobResult
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class AggregatePipeline(name: String): AbstractPipeline(name) {

    val generatedItemAcks: MutableMap<UUID, ItemAck> = ConcurrentHashMap()

    val generatedItemIds: MutableList<UUID> = mutableListOf()

    abstract fun accumulateItem(item: Item)

    abstract fun aggregate(): List<Item>

    override suspend fun processItem(item: Item): List<Item> {
        try {
            accumulateItem(item)
            val ack = item.buildBack()
            outChannel.send(ack)
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            val back = item.buildErrorBack(error)
            outChannel.send(back)
        }
        return emptyList()
    }



    override suspend fun preEnd(event: Event): EventJobResult? {
        println(event)
        val items = aggregate() as MutableList
        generatedItemIds.addAll(items.map{it.channelableId})
        items.forEach { outChannel.send(it) }
        return null
    }

    override suspend fun postEnd(event: EventBack) {
        if(generatedItemIds.size != generatedItemAcks.size){
            event.status += Status.ERROR
            val error = ErrorInfo(Exception("There are some item that are not acked"), this, ErrorLevel.MAJOR)
            event.errorInfos.add(error)
        }
        generatedItemAcks.forEach { (_, value) ->
            event.status += value.status
            event.errorInfos.addAll(value.errorInfos)
        }
    }

    override suspend fun processItemAck(itemAck: ItemAck) {
        generatedItemAcks[itemAck.channelableId] = itemAck
    }

}