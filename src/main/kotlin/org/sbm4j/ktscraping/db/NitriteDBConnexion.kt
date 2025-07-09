package org.sbm4j.ktscraping.db

import org.dizitart.kno2.documentOf
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.dizitart.kno2.serialization.KotlinXSerializationMapper
import org.dizitart.no2.Nitrite
import org.dizitart.no2.common.module.NitriteModule.module
import org.dizitart.no2.mvstore.MVStoreModule
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.data.item.DataItem
import java.io.File
import kotlin.reflect.KProperty1

class NitriteDBConnexion(dbFile: File): DBConnexion{
    companion object{
        private val dbs: MutableMap<String, Nitrite> = mutableMapOf()
    }

    val db: Nitrite


    init {
        if(!dbFile.exists()){
            dbFile.createNewFile()
        }

        val path = dbFile.absoluteFile.path
        db = dbs.getOrPut(path) { buildNitriteDB(dbFile) }
    }

    fun buildNitriteDB(file: File): Nitrite{
        val storeModule = MVStoreModule.withConfig()
            .filePath(file)
            .build()

        return nitrite {
            loadModule(storeModule)
            loadModule(module(KotlinXSerializationMapper()))
        }
    }

    override fun <T> getKeys(classObject: Class<T>, keyProperty: KProperty1<T, *>): Set<*>{
        val repo = db.getRepository(classObject)
        val cursor = repo.find()
        return cursor.map { keyProperty.get(it) } as Set<*>
    }

    override fun clear(classObject: Class<*>) {
        val repository = db.getRepository(classObject)
        repository.clear()
    }

    override fun perfomInsertItem(item: DataItem<*>) {
        val data = item.data
        val repository = db.getRepository(data.javaClass)
        repository.insert(data)
    }

    override fun performItemUpdate(item: ItemUpdate) {
        val repository = db.getRepository(item.entityType)
        val doc = documentOf()
        item.values.forEach {
            doc.put(it.key, it.value)
        }
        repository.update(item.keyName eq item.keyValue, doc)
    }

    override fun performItemDelete(item: ItemDelete) {
        val repository = db.getRepository(item.entityType)
        repository.remove(item.keyName eq item.keyValue)
    }

    override fun commit() {
        db.commit()
    }

    override fun close() {
        db.commit()
        db.close()
    }

    override fun <T> getObjects(classObject: Class<T>): List<T> {
        val repository = db.getRepository(classObject)
        val cursor = repository.find()
        return cursor.map { it }
    }

    override fun getSize(classObject: Class<*>): Long {
        val repository = db.getRepository(classObject)
        return repository.size()
    }

}