package org.sbm4j.ktscraping.db

import org.dizitart.kno2.documentOf
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.dizitart.kno2.serialization.KotlinXSerializationMapper
import org.dizitart.no2.Nitrite
import org.dizitart.no2.common.module.NitriteModule
import org.dizitart.no2.mvstore.MVStoreModule
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.db.NitriteDBConnexion.Companion.dbs
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * [org.sbm4j.ktscraping.db.DBConnexion] implementation backed by a [org.dizitart.no2.Nitrite] embedded document database.
 *
 * Nitrite is a serverless, embedded document store that persists Kotlin objects
 * directly via [org.dizitart.kno2.serialization.KotlinXSerializationMapper], making it ideal for local scraping
 * sessions without requiring an external database server.
 *
 * Implements a multiton pattern via the [dbs] companion map: multiple
 * [NitriteDBConnexion] instances pointing to the same file share the same
 * underlying [org.dizitart.no2.Nitrite] instance, preventing concurrent access conflicts on
 * the same file.
 *
 * @param dbFile The file to use as the Nitrite database store.
 * If it does not exist, it is created automatically.
 *
 * @see org.sbm4j.ktscraping.db.DBConnexion
 */
class NitriteDBConnexion(dbFile: File): DBConnexion{
    companion object{
        /**
         * Multiton cache of [org.dizitart.no2.Nitrite] instances keyed by absolute file path.
         * Ensures that multiple [NitriteDBConnexion] instances pointing to the
         * same file share a single [org.dizitart.no2.Nitrite] instance.
         */
        private val dbs: MutableMap<String, Nitrite> = mutableMapOf()

        /**
         * Clears the multiton cache, closing all cached [Nitrite] instances.
         * Useful for resetting state between tests.
         */
        fun reset(){
            dbs.clear()
        }
    }

    /** The underlying [Nitrite] database instance for this connection. */
    val db: Nitrite


    init {
        if(!dbFile.exists()){
            dbFile.createNewFile()
        }

        val path = dbFile.absoluteFile.path
        db = dbs.getOrPut(path) { buildNitriteDB(dbFile) }
    }

    /**
     * Builds a new [Nitrite] instance backed by an MVStore file at the given [file] path,
     * with [org.dizitart.kno2.serialization.KotlinXSerializationMapper] for direct Kotlin object serialization.
     *
     * @param file The database file to use as the MVStore backend.
     * @return A fully configured [Nitrite] instance.
     */
    fun buildNitriteDB(file: File): Nitrite {
        val storeModule = MVStoreModule.withConfig()
            .filePath(file)
            .build()

        return nitrite {
            loadModule(storeModule)
            loadModule(NitriteModule.module(KotlinXSerializationMapper()))
        }
    }

    /**
     * Retrieves the set of existing key values for a given class and key property.
     *
     * Opens the Nitrite repository for [classObject], scans all records, and
     * extracts the value of [keyProperty] for each. Used by [DBSyncMiddleware]
     * to detect already-stored records and avoid redundant downloads.
     *
     * @param T The type of the domain object.
     * @param classObject The [Class] of the domain object.
     * @param keyProperty The property to extract as the key.
     * @return The set of existing key values.
     */
    override fun <T: Any> getKeys(classObject: KClass<T>, keyProperty: KProperty1<T, *>): Set<*>{
        val repo = db.getRepository(classObject.java)
        val cursor = repo.find()
        return cursor.map { keyProperty.get(it) }.toSet()
    }

    /**
     * Clears all records of [classObject] from the Nitrite repository.
     *
     * @param classObject The [Class] of the domain object to clear.
     */
    override fun <T: Any> clear(classObject: KClass<T>) {
        val repository = db.getRepository(classObject.java)
        repository.clear()
    }

    /**
     * Inserts the data object carried by [item] into its corresponding Nitrite repository.
     * The insert is not persisted until [commit] is called.
     *
     * @param item The item whose [org.sbm4j.ktscraping.data.item.ObjectDataItem.data] object to insert.
     */
    override fun performInsertItem(item: ObjectDataItem<*>): Boolean {
        val data = item.data
        val repository = db.getRepository(data.javaClass)
        val result = repository.insert(data)
        return result.affectedCount >= 1
    }

    /**
     * Applies a partial update to the record matching [ItemUpdate.keyName] = [ItemUpdate.keyValue]
     * in the Nitrite repository for [ItemUpdate.entityType].
     *
     * Uses Nitrite's filter syntax (`keyName eq data`) to locate the target record,
     * then applies the [ItemUpdate.values] map as a document patch.
     * The update is not persisted until [commit] is called.
     *
     * @param item The partial update to apply.
     */
    override fun <T: Any> performItemUpdate(item: ItemUpdate<T>): Boolean {
        val repository = db.getRepository(item.entityType.java)
        val doc = documentOf()
        item.values.forEach {
            doc.put(it.key, it.value)
        }
        val result = repository.update(item.keyName eq item.keyValue, doc)
        return result.affectedCount >= 1
    }

    /**
     * Removes the record matching [ItemDelete.keyName] = [ItemDelete.keyValue]
     * from the Nitrite repository for [ItemDelete.entityType].
     *
     *
     * @param item The delete operation to apply.
     */
    override fun <T: Any> performItemDelete(item: ItemDelete<T>): Boolean {
        val repository = db.getRepository(item.entityType.java)
        val result = repository.remove(item.keyName eq item.keyValue)
        return result.affectedCount >= 1
    }

    /**
     * Commits all pending operations to the Nitrite database file.
     *
     * Should be called periodically or at the end of the scraping session
     * to flush buffered inserts, updates, and deletes.
     */
    override fun commit() {
        db.commit()
    }

    /**
     * Commits all pending operations and closes the Nitrite database.
     *
     * Called automatically at the end of the scraping session via
     * [EventProcessor.preEnd]. Also commits before closing to ensure
     * no data is lost.
     */
    override fun close() {
        db.commit()
        db.close()
    }

    /**
     * Retrieves all records of [classObject] from the Nitrite repository.
     *
     * @param T The type of the domain object.
     * @param classObject The [Class] of the domain object to retrieve.
     * @return A list of all stored objects of type [T].
     */
    override fun <T: Any> getObjects(classObject: KClass<T>): List<T> {
        val repository = db.getRepository(classObject.java)
        val cursor = repository.find()
        return cursor.map { it }
    }

    /**
     * Returns the number of records of [classObject] stored in the Nitrite repository.
     *
     * @param classObject The [Class] of the domain object to count.
     * @return The total number of stored records.
     */
    override fun <T: Any> getSize(classObject: KClass<T>): Long {
        val repository = db.getRepository(classObject.java)
        return repository.size()
    }

}