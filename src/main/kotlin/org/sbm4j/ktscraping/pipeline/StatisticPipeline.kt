package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.data.item.Item

abstract class StatisticPipeline(name: String): AggregatePipeline(name) {

    val items: MutableList<Item> = mutableListOf()

    override fun accumulateItem(item: Item) {
        items.add(item)
    }

}