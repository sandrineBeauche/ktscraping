package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Item
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



class DBExporter(name: String): AbstractExporter(name), DBControllable {

    override lateinit var db: DBConnexion

    override fun exportItem(item: Item) {
        performDBItem(item)
    }

    override suspend fun stop() {
        super.stop()
        db.commit()
    }
}