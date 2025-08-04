package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.DataItemAck

class FilterPipeline(name: String) : AbstractPipeline(name) {

    lateinit var filteringFunc: (Any) -> Boolean

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        val result = filteringFunc(item)
        return if (result) {
            listOf(item)
        } else {
            val ack = DataItemAck(item.itemId, Status.IGNORED)
            itemAckOut.send(ack)
            emptyList()
        }
    }
}