package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.StandardFormatItem

class StdOutExporter(name: String = "StdOutExporter") : AbstractExporter(name) {
    override fun exportItem(item: Item) {
        when(item){
            is DataItem<*> -> println(item.data)
            is StandardFormatItem<*> -> println(item.prettyPrint())
        }
    }
}