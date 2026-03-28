package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.item.Item

class FilterPipeline(name: String) : AbstractPipeline(name) {

    lateinit var filteringFunc: (Any) -> Boolean

    override suspend fun processItem(item: Item): List<Item> {
        val result = filteringFunc(item)
        return if (result) {
            listOf(item)
        } else {
            val ack = item.buildBack()
            ack.status = Status.IGNORED
            outChannel.send(ack)
            emptyList()
        }
    }
}