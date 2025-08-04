package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.StandardFormatItem

class StdOutExporter(name: String = "StdOutExporter") : AbstractExporter(name) {
    override suspend fun exportItem(item: DataItem<*>) {
        when(item){
            is ObjectDataItem<*> -> println(item.data)
            is StandardFormatItem<*> -> println(item.prettyPrint())
        }
    }
}