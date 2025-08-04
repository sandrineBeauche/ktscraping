package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.EventJobResult
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Item
import kotlin.reflect.KProperty

data class ItemUpdate(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val values: Map<String, Any>,
    val label: String = "update",
): DataItem<Any>() {
    override fun clone(): Item {
        return this.copy()
    }
}

data class ItemDelete(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val label: String = "delete"
): DataItem<Any>() {
    override fun clone(): Item {
        return this.copy()
    }
}



class DBExporter(name: String): AbstractExporter(name), DBControllable {

    override lateinit var db: DBConnexion

    override suspend fun exportItem(item: DataItem<*>) {
        performDBItem(item)
    }

    override suspend fun preEnd(event: Event): EventJobResult? {
        db.commit()
        return super.preEnd(event)
    }
}