package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ItemError
import org.sbm4j.ktscraping.data.item.ItemStatus
import java.util.UUID

abstract class AccumulatePipeline(name: String): AbstractPipeline(name) {

    val itemIds: MutableList<UUID> = mutableListOf()

    lateinit var endAckId: UUID

    abstract fun accumulateItem(item: Item)

    abstract fun generateItems(): List<Item>

    override suspend fun processItem(item: Item): List<Item> {
        return when(item){
            is EndItem -> processItemEnd(item)
            is DataItem<*> -> processDataItem(item)
            else -> emptyList()
        }
    }

    fun processItemEnd(item: EndItem): List<Item>{
        val items = generateItems() as MutableList
        itemIds.addAll(items.map{it.itemId})
        items.addLast(item)
        endAckId = item.itemId
        return items
    }

    suspend fun processDataItem(item: DataItem<*>): List<Item>{
        try {
            accumulateItem(item)
            itemAckOut.send(ItemAck(item.itemId, ItemStatus.PROCESSED))
        }
        catch(ex: Exception){
            val error = ItemError(ErrorInfo(ex, this, ErrorLevel.MAJOR))
            itemAckOut.send(ItemAck(item.itemId,
                ItemStatus.ERROR, listOf(error)))
        }
        return emptyList()
    }

    override suspend fun performAck(itemAck: ItemAck) {
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