package org.sbm4j.ktscraping.pipeline

import kotlinx.coroutines.Job
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
import java.util.*

abstract class AccumulatePipeline(name: String): AbstractPipeline(name) {

    val itemIds: MutableList<UUID> = mutableListOf()

    lateinit var endAckId: UUID

    abstract fun accumulateItem(item: DataItem<*>)

    abstract fun generateItems(): List<Item>

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        try {
            accumulateItem(item)
            itemAckOut.send(ItemAck(item.itemId, Status.OK))
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            itemAckOut.send(ItemAck(item.itemId,
                Status.ERROR, mutableListOf(error)))
        }
        return emptyList()
    }

    fun processItemEnd(item: EndItem): List<Item>{
        val items = generateItems() as MutableList
        itemIds.addAll(items.map{it.itemId})
        items.addLast(item)
        endAckId = item.itemId
        return items
    }

    override suspend fun performAck(itemAck: AbstractItemAck) {
        if(itemAck.itemId == endAckId) {
            if(itemIds.isNotEmpty()){
                logger.warn { "${name}: Some item data are not acked" }
            }
            super.performAck(itemAck)
        }
        else{
            itemIds.remove(itemAck.itemId)
        }
    }
}