package org.sbm4j.ktscraping.exporters

import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.reflect.KProperty

/**
 * An [Item] representing a partial update operation on an existing database record.
 *
 * Instead of resending a full [ObjectDataItem], [ItemUpdate] carries only the fields
 * that need to be modified ([values]), along with the information needed to locate
 * the target record ([entityType], [keyName], [data]).
 *
 * Typically emitted by a Spider after [DBSyncMiddleware] signals that a record already
 * exists in the database, allowing the Spider to update only the changed fields without
 * triggering a full download.
 *
 * @property entityType The [Class] of the domain object to update in the database.
 * @property keyName The property used as the lookup key to identify the target record.
 * @property data The value of [keyName] identifying the record to update.
 * @property values Map of field names to their new values to apply as a partial update.
 * @property label A descriptive label for this update, defaults to `"update"`.
 * @property sender The Spider emitting this update.
 * @property name Technical name of this item, auto-generated as `"<label>-<entityType>-<id>"`.
 *
 * @see DBConnexion.performItemUpdate
 * @see DBControllable
 * @see ItemDelete
 */
data class ItemUpdate(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val values: Map<String, Any>,
    val label: String = "update",
    override var sender: SendSource,
    override val name: String = "${label}-${entityType.simpleName}-${lastId.getAndIncrement()}"
): DataItem<Any>() {
    /** Creates a copy of this item via [copy]. */
    override fun clone(): Item {
        return this.copy()
    }

    /**
     * Returns `"<label>-<entityType simple name>"` as the barrier key, allowing
     * [Barrier] nodes to synchronize branches by entity type and update label.
     */
    override fun getKeyBarrier(): String {
        return "$label-${entityType.simpleName}"
    }
}

/**
 * An [Item] representing a delete operation on an existing database record.
 *
 * Carries the information needed to locate and remove the target record
 * ([entityType], [keyName], [data]) without resending the full object.
 *
 * Typically emitted by a Spider when a previously scraped record no longer exists
 * at the source and should be removed from the database.
 *
 * @property entityType The [Class] of the domain object to delete from the database.
 * @property keyName The property used as the lookup key to identify the target record.
 * @property data The value of [keyName] identifying the record to delete.
 * @property label A descriptive label for this deletion, defaults to `"delete"`.
 * @property sender The Spider emitting this deletion.
 * @property name Technical name of this item, auto-generated as `"<label>-<entityType>-<id>"`.
 *
 * @see DBConnexion.performItemDelete
 * @see DBControllable
 * @see ItemUpdate
 */
data class ItemDelete(
    val entityType: Class<*>,
    val keyName: KProperty<*>,
    override val data: Any,
    val label: String = "delete",
    override var sender: SendSource,
    override val name: String = "${label}-${entityType.simpleName}-${lastId.getAndIncrement()}"
): DataItem<Any>() {
    /** Creates a copy of this item via [copy]. */
    override fun clone(): Item {
        return this.copy()
    }

    /**
     * Returns `"<label>-<entityType simple name>"` as the barrier key, allowing
     * [Barrier] nodes to synchronize branches by entity type and deletion label.
     */
    override fun getKeyBarrier(): String {
        return "$label-${entityType.simpleName}"
    }
}


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
        performDBItem(item)
    }

    override suspend fun preEnd(event: Event): EventJobResult? {
        db.commit()
        return super.preEnd(event)
    }
}