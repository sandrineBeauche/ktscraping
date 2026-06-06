package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.db.getObjects
import kotlin.reflect.KClass

class DBPipeline<T: Data>(name: String): AggregatePipeline(name), DBControllable {

    override lateinit var db: DBConnexion

    lateinit var objectClass: KClass<T>

    override fun accumulateItem(item: Item) {
        performDBItem(item)
    }

    override fun aggregate(): List<Item> {
        val values = db.getObjects(objectClass)
        val items =
            values.map{
                ObjectDataItem(
                    it, objectClass,
                    objectClass.simpleName!!,
                    sender = this
                )
            } as MutableList<Item>
        return items
    }
}