package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.requests.Data
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemStatus

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