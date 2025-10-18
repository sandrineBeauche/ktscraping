package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

class MapPipeline(name: String) : AbstractPipeline(name) {

    lateinit var mapFunc: (Item) -> Item

    override suspend fun processItem(item: Item): List<Item> {
        val result = mapFunc(item)
        return listOf(result)
    }
}