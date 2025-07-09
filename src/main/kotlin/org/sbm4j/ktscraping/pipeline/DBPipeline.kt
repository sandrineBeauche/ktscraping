package org.sbm4j.ktscraping.pipeline

import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

class DBPipeline<T: Data>(name: String): AccumulatePipeline(name), DBControllable {

    override lateinit var db: DBConnexion

    lateinit var objectClass: Class<T>

    override fun accumulateItem(item: Item) {
        performDBItem(item)
    }

    override fun generateItems(): List<Item> {
        val values = db.getObjects(objectClass)
        val items =
            values.map{
                DataItem(
                    it, objectClass.kotlin,
                    objectClass.canonicalName
                )
            } as MutableList<Item>
        return items
    }
}