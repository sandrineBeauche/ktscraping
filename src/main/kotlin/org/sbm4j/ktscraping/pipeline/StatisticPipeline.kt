package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

abstract class StatisticPipeline(name: String): AccumulatePipeline(name) {

    val items: MutableList<DataItem<*>> = mutableListOf()

    override fun accumulateItem(item: Item) {
        if(item is DataItem<*>){
            items.add(item)
        }
    }

}