package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import java.util.*
import kotlin.reflect.KProperty

data class ItemUpdate(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    val keyValue: Any,
    val values: Map<String, Any>,
    override val itemId: UUID = UUID.randomUUID()
): Item {
    override fun clone(): Item {
        return this.copy()
    }
}

data class ItemDelete(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    val keyValue: Any,
    override val itemId: UUID = UUID.randomUUID()
): Item {
    override fun clone(): Item {
        return this.copy()
    }
}



abstract class DBExporter(scope: CoroutineScope, name: String): AbstractExporter(scope, name) {
    override fun exportItem(item: Item) {
        when(item){
            is ItemUpdate -> performItemUpdate(item)
            is ItemDelete -> performItemDelete(item)
            is DataItem -> perfomInsertItem(item)
        }
    }

    abstract fun performItemUpdate(item: ItemUpdate)

    abstract fun performItemDelete(item: ItemDelete)

    abstract fun perfomInsertItem(item: DataItem)
}