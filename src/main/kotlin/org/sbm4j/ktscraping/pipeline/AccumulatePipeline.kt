package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.EventJobResult
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class AccumulatePipeline(name: String): AbstractPipeline(name) {

    val generatedItemAcks: MutableMap<UUID, DataItemAck> = ConcurrentHashMap()

    val generatedItemIds: MutableList<UUID> = mutableListOf()

    abstract fun accumulateItem(item: DataItem<*>)

    abstract fun generateItems(): List<Item>

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        try {
            accumulateItem(item)
            itemAckOut.send(DataItemAck(item.itemId, Status.OK))
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            itemAckOut.send(DataItemAck(item.itemId,
                Status.ERROR, mutableListOf(error)))
        }
        return emptyList()
    }



    override suspend fun preEnd(event: Event): EventJobResult? {
        println(event)
        val items = generateItems() as MutableList
        generatedItemIds.addAll(items.map{it.itemId})
        items.forEach { itemOut.send(it) }
        return null
    }

    override suspend fun postEnd(event: EventBack) {
        if(generatedItemIds.size != generatedItemAcks.size){
            event.status = event.status + Status.ERROR
            val error = ErrorInfo(Exception("There are some item that are not acked"), this, ErrorLevel.MAJOR)
            event.errorInfos.add(error)
        }
        generatedItemAcks.forEach { key, value ->
            event.status = event.status + value.status
            event.errorInfos.addAll(value.errorInfos)
        }
    }

    override suspend fun performDataAck(itemAck: DataItemAck) {
        generatedItemAcks[itemAck.itemId] = itemAck
    }


}