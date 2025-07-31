package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item

class MapPipeline(name: String) : AbstractPipeline(name) {

    lateinit var mapFunc: (ObjectDataItem<*>) -> ObjectDataItem<*>

    override suspend fun processDataItem(item: ObjectDataItem<*>): List<Item> {
        val result = mapFunc(item)
        return listOf(result)
    }
}