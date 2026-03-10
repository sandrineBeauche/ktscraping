package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.meercat.components.EventJobResult
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.components.SendSource
import kotlin.reflect.KProperty

data class ItemUpdate(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val values: Map<String, Any>,
    val label: String = "update",
    override var sender: SendSource,
    override val name: String = "${label}-${entityType.simpleName}-${lastId.getAndIncrement()}"
): DataItem<Any>() {
    override fun clone(): Item {
        return this.copy()
    }
}

data class ItemDelete(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val label: String = "delete",
    override var sender: SendSource,
    override val name: String = "${label}-${entityType.simpleName}-${lastId.getAndIncrement()}"
): DataItem<Any>() {
    override fun clone(): Item {
        return this.copy()
    }
}



class DBExporter(name: String): AbstractExporter(name), DBControllable {

    override lateinit var db: DBConnexion

    override suspend fun exportItem(item: Item) {
        performDBItem(item)
    }

    override suspend fun preEnd(event: Event): EventJobResult? {
        db.commit()
        return super.preEnd(event)
    }
}