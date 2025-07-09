package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ItemStatus

class FilterPipeline(name: String) : AbstractPipeline(name) {

    lateinit var filteringFunc: (Data) -> Boolean

    override suspend fun processItem(item: Item): List<Item> {
        if(item is DataItem<*>) {
            val result = filteringFunc(item.data)
            return if (result) {
                listOf(item)
            } else {
                val ack = ItemAck(item.itemId, ItemStatus.IGNORED)
                itemAckOut.send(ack)
                emptyList()
            }
        }
        else {
            return listOf(item)
        }
    }
}