package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable


/**
 * A terminal Pipeline node that persists scraped [Item]s to a [DBConnexion].
 *
 * Combines [AbstractExporter] and [DBControllable] to export items directly to
 * a database. Each incoming [Item] is dispatched to the appropriate database
 * operation via [DBControllable.performDBItem]:
 * - [ObjectDataItem] → insert
 * - [ItemUpdate] → partial update
 * - [ItemDelete] → delete
 *
 * Pending operations are committed when the [EndEvent] is received via [preEnd],
 * ensuring all buffered writes are flushed before the topology shuts down.
 *
 * The [db] connection must be injected before the crawler starts, typically via
 * the DSL configuration block.
 *
 * @param name The name of this exporter node.
 *
 * @see AbstractExporter
 * @see DBControllable
 * @see DBConnexion
 */
class DBExporter(name: String): AbstractExporter(name), DBControllable {

    override lateinit var db: DBConnexion

    override suspend fun exportItem(item: Item) {
        val result = performDBItem(item)
        if(!result){

        }
    }

    override suspend fun preEnd(event: Event): EventJobResult? {
        db.commit()
        return super.preEnd(event)
    }
}