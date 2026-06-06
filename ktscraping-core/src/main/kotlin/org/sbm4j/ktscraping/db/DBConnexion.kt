package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Abstraction layer for database connections in the KtScraping topology.
 *
 * A [DBConnexion] wraps a database connection and exposes a unified API for
 * reading and writing scraped data, regardless of the underlying database technology
 * (SQL, NoSQL, etc.). Multiple components (Spiders, middlewares, exporters, pipelines)
 * can share the same [DBConnexion] instance, which is declared once at the crawler level.
 *
 * Operations ([performInsertItem], [performItemUpdate], [performItemDelete]) are
 * buffered until [commit] is called explicitly, allowing batch writes for better
 * performance.
 *
 * The connection lifecycle (open/close) is managed via the [preStart]/[preEnd] hooks
 * of [EventProcessor], ensuring the connection is ready before scraping starts and
 * properly closed when it ends.
 *
 * @see DBSyncMiddleware
 * @see AbstractExporter
 */
interface DBConnexion {

    /**
     * Retrieves the set of existing keys for a given class and key property.
     *
     * Typically used by [DBSyncMiddleware] to check which records already exist
     * in the database, allowing the Spider to skip unnecessary downloads or emit
     * [ItemUpdate]s instead of full inserts.
     *
     * @param T The type of the domain object.
     * @param classObject The [Class] of the domain object.
     * @param keyProperty The property to use as the key (e.g. an ID or URL field).
     * @return The set of existing key values for this class.
     */
    fun <T: Any> getKeys(classObject: KClass<T>, keyProperty: KProperty1<T, *>): Set<*>

    /**
     * Clears all records of the given class from the database.
     *
     * @param classObject The [Class] of the domain object to clear.
     */
    fun <T: Any> clear(classObject: KClass<T>)

    /**
     * Buffers an insert operation for the given [ObjectDataItem].
     * The insert is not committed until [commit] is called.
     *
     * @param item The item to insert.
     * @return true if the insert succeeds, false otherwise
     */
    fun performInsertItem(item: ObjectDataItem<*>): Boolean

    /**
     * Buffers an update operation for the given [ItemUpdate].
     * The update is not committed until [commit] is called.
     *
     * @param item The partial update to apply.
     * @return true if the update succeeds, false otherwise
     */
    fun <T: Any> performItemUpdate(item: ItemUpdate<T>): Boolean

    /**
     * Buffers a delete operation for the given [ItemDelete].
     * The delete is not committed until [commit] is called.
     *
     * @param item The delete operation to apply.
     * @return true if the delete succeeds, false otherwise
     */
    fun <T: Any> performItemDelete(item: ItemDelete<T>): Boolean

    /**
     * Commits all buffered operations ([performInsertItem], [performItemUpdate],
     * [performItemDelete]) to the database.
     *
     * Should be called periodically or at the end of a scraping session
     * (typically in [EventProcessor.preEnd]) to flush pending writes.
     */
    fun commit()

    /**
     * Closes the database connection and releases all associated resources.
     *
     * Called automatically at the end of the scraping session via
     * [EventProcessor.preEnd].
     */
    fun close()

    /**
     * Retrieves all records of the given class from the database.
     *
     * @param T The type of the domain object.
     * @param classObject The [Class] of the domain object to retrieve.
     * @return A list of all stored objects of type [T].
     */
    fun <T: Any> getObjects(classObject: KClass<T>): List<T>

    /**
     * Returns the number of records of the given class stored in the database.
     *
     * @param classObject The [Class] of the domain object to count.
     * @return The number of stored records.
     */
    fun <T: Any> getSize(classObject: KClass<T>): Long

}

/**
 * Convenience reified overload of [DBConnexion.getSize] for the type [T].
 */
inline fun <reified T: Any> DBConnexion.getSize(): Long {
    return this.getSize(T::class)
}

/**
 * Convenience reified overload of [DBConnexion.getObjects] for the type [T].
 */
inline fun <reified T: Any> DBConnexion.getObjects(): List<T> {
    return this.getObjects(T::class)
}

/**
 * Convenience reified overload of [DBConnexion.getKeys] for the type [T].
 */
inline fun <reified T: Any> DBConnexion.getKeys(keyProperty: KProperty1<T, *>): Set<*>{
    return this.getKeys(T::class, keyProperty)
}

/**
 * Convenience reified overload of [DBConnexion.clear] for the type [T].
 */
inline fun <reified T: Any> DBConnexion.clear() {
    this.clear(T::class)
}