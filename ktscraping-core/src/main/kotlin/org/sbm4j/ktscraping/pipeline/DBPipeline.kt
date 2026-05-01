package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item

class DBPipeline<T: Data>(name: String): AggregatePipeline(name), DBControllable {

    override lateinit var db: DBConnexion

    lateinit var objectClass: Class<T>

    override fun accumulateItem(item: Item) {
        performDBItem(item)
    }

    override fun aggregate(): List<Item> {
        val values = db.getObjects(objectClass)
        val items =
            values.map{
                ObjectDataItem(
                    it, objectClass.kotlin,
                    objectClass.canonicalName,
                    sender = this
                )
            } as MutableList<Item>
        return items
    }
}