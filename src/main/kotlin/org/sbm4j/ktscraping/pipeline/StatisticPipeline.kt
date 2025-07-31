package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item

abstract class StatisticPipeline(name: String): AccumulatePipeline(name) {

    val items: MutableList<ObjectDataItem<*>> = mutableListOf()

    override fun accumulateItem(item: ObjectDataItem<*>) {
        items.add(item)
    }

}