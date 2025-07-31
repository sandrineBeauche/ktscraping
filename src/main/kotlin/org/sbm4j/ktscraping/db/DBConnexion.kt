package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import kotlin.reflect.KProperty1

interface DBConnexion {

    fun <T> getKeys(classObject: Class<T>, keyProperty: KProperty1<T, *>): Set<*>

    fun clear(classObject: Class<*>)

    fun perfomInsertItem(item: ObjectDataItem<*>)

    fun performItemUpdate(item: ItemUpdate)

    fun performItemDelete(item: ItemDelete)

    fun commit()

    fun close()

    fun <T> getObjects(classObject: Class<T>): List<T>

    fun getSize(classObject: Class<*>): Long
}