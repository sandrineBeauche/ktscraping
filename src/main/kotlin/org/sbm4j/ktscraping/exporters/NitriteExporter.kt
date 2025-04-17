package org.sbm4j.ktscraping.exporters

import org.dizitart.kno2.documentOf
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.dizitart.kno2.serialization.KotlinXSerializationMapper
import org.dizitart.no2.Nitrite
import org.dizitart.no2.common.module.NitriteModule.module
import org.dizitart.no2.mvstore.MVStoreModule
import org.sbm4j.ktscraping.requests.DataItem
import java.io.File

fun buildNitriteDB(file: File): Nitrite{
    val storeModule = MVStoreModule.withConfig()
        .filePath(file)
        .build()

    return nitrite {
        loadModule(storeModule)
        loadModule(module(KotlinXSerializationMapper()))
    }
}


class NitriteExporter(name: String): DBExporter(name) {

    lateinit var db: Nitrite

    override suspend fun stop() {
        super.stop()
        db.commit()
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

    override fun perfomInsertItem(item: DataItem<*>) {
        val data = item.data
        val repository = db.getRepository(data.javaClass)
        repository.insert(data)
    }
}