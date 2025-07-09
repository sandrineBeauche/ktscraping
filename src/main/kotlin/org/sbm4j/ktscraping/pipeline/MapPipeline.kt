package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

class MapPipeline(name: String) : AbstractPipeline(name) {

    lateinit var mapFunc: (DataItem<*>) -> DataItem<*>

    override suspend fun processItem(item: Item): List<Item> {
        if(item is DataItem<*>){
            val result = mapFunc(item)
            return listOf(result)
        }
        else{
            return listOf(item)
        }
    }
}