package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.db.DBControllable
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Abstract base class for CRUD-related [Item] messages, carrying the information needed
 * to identify and manipulate a database record of type [T].
 *
 * Provides a [getKeyBarrier] implementation based on [label] and [entityType], allowing
 * [Barrier] nodes to synchronise branches by entity type and operation label.
 *
 * @param T the type of the domain object targeted by this CRUD operation
 * @property entityType the [KClass] of the domain object to manipulate in the database
 */
abstract class CRUDItem<T: Any>(open val entityType: KClass<T>): Item(){
    /**
     * The property used as the lookup key to identify the target record.
     */
    abstract val keyName: KProperty1<T, *>

    /**
     * The value of [keyName] identifying the target record.
     */
    abstract val keyValue: Any

    /**
     * A descriptive label for this operation, used in [getKeyBarrier] and [name] generation.
     */
    abstract val label: String

    /**
     * Returns `"<label>-<entityType simple name>"` as the barrier key, allowing
     * [Barrier] nodes to synchronize branches by entity type and operation label.
     *
     * @see Send.getKeyBarrier
     */
    override fun getKeyBarrier(): String {
        return "$label-${entityType.simpleName}"
    }

    /**
     * Generates a unique technical name for this item in the format
     * `"<label>-<entityName>-<id>"`.
     *
     * @param entityName the simple name of the entity type
     * @return a unique name for this item
     */
    fun generateName(entityName: String): String{
        return "$label-${entityName}-${lastId.getAndIncrement()}"
    }
}

/**
 * Abstract base implementation of [CRUDItem] for partial update operations,
 * setting [label] to `"update"` and auto-generating [name].
 *
 * @param T the type of the domain object to update
 */
abstract class AbstractItemUpdate<T: Any>(entityType: KClass<T>): CRUDItem<T>(entityType){
    /**
     * Always `"update"` for update operations.
     */
    override val label: String = "update"

    /**
     * Auto-generated as `"update-<entityType>-<id>"`.
     */
    override val name: String = generateName(entityType.simpleName!!)
}

/**
 * An [Item] representing a partial update operation on an existing database record.
 *
 * Instead of resending a full object, [ItemUpdate] carries only the fields that need
 * to be modified ([values]), along with the information needed to locate the target
 * record ([entityType], [keyName], [keyValue]).
 *
 * Typically emitted by a Spider after [DBSyncMiddleware] signals that a record already
 * exists in the database, allowing the Spider to update only the changed fields without
 * triggering a full download.
 *
 * @param T the type of the domain object to update
 * @property entityType the [KClass] of the domain object to update in the database
 * @property keyName the property used as the lookup key to identify the target record
 * @property keyValue the value of [keyName] identifying the record to update
 * @property values map of field names to their new values to apply as a partial update
 * @property sender the [SendSource] emitting this update
 *
 * @see DBConnexion.performItemUpdate
 * @see DBControllable
 * @see ItemDelete
 */
data class ItemUpdate<T: Any>(
    override val entityType: KClass<T>,
    override val keyName: KProperty1<T, *>,
    override val keyValue: Any,
    val values: Map<String, Any>,
    override var sender: SendSource,
): AbstractItemUpdate<T>(entityType) {
    /**
     * @see Item.clone
     */
    override fun clone(): Item {
        return this.copy()
    }
}

/**
 * Abstract base implementation of [CRUDItem] for delete operations,
 * setting [label] to `"delete"` and auto-generating [name].
 *
 * @param T the type of the domain object to delete
 */
abstract class AbstractItemDelete<T: Any>(entityType: KClass<T>): CRUDItem<T>(entityType){
    /**
     * Always `"delete"` for delete operations.
     */
    override val label: String = "delete"

    /**
     * Auto-generated as `"delete-<entityType>-<id>"`.
     */
    override val name: String = generateName(entityType.simpleName!!)
}

/**
 * An [Item] representing a delete operation on an existing database record.
 *
 * Carries the information needed to locate and remove the target record
 * ([entityType], [keyName], [keyValue]) without resending the full object.
 *
 * Typically emitted by a Spider when a previously scraped record no longer exists
 * at the source and should be removed from the database.
 *
 * @param T the type of the domain object to delete
 * @property entityType the [KClass] of the domain object to delete from the database
 * @property keyName the property used as the lookup key to identify the target record
 * @property keyValue the value of [keyName] identifying the record to delete
 * @property sender the [SendSource] emitting this deletion
 *
 * @see DBConnexion.performItemDelete
 * @see DBControllable
 * @see ItemUpdate
 */
data class ItemDelete<T: Any>(
    override val entityType: KClass<T>,
    override val keyName: KProperty1<T, *>,
    override val keyValue: Any,
    override var sender: SendSource,
): AbstractItemDelete<T>(entityType) {
    /**
     * @see Item.clone
     */
    override fun clone(): Item {
        return this.copy()
    }
}