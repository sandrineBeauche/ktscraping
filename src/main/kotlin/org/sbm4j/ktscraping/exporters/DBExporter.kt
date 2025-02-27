package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import kotlin.reflect.KProperty

data class ItemUpdate(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    val keyValue: Any,
    val values: Map<String, Any>,
    val label: String = "update"
): Item() {
    override fun clone(): Item {
        return this.copy()
    }
}

data class ItemDelete(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    val keyValue: Any,
    val label: String = "delete"
): Item() {
    override fun clone(): Item {
        return this.copy()
    }
}



abstract class DBExporter(name: String): AbstractExporter(name) {
    override fun exportItem(item: Item) {
        when(item){
            is ItemUpdate -> {
                logger.debug { "${name}: update item ${item}" }
                performItemUpdate(item)
            }
            is ItemDelete -> {
                logger.debug { "${name}: delete item ${item}"}
                performItemDelete(item)
            }
            is DataItem -> {
                logger.debug { "${name}: insert item ${item}"}
                perfomInsertItem(item)
            }
        }
    }

    abstract fun performItemUpdate(item: ItemUpdate)

    abstract fun performItemDelete(item: ItemDelete)

    abstract fun perfomInsertItem(item: DataItem)
}