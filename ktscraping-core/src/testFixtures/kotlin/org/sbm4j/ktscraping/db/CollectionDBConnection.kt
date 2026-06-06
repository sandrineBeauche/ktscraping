package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class CollectionDBConnection(): DBConnexion{

    val data: MutableList<Any> = mutableListOf()

    override fun <T: Any> getKeys(
        classObject: KClass<T>,
        keyProperty: KProperty1<T, *>
    ): Set<*> {
        return data
            .filterIsInstance(classObject.java)
            .map { keyProperty.get(it) }
            .toSet()
    }

    override fun <T: Any> clear(classObject: KClass<T>) {
        val items = data.filterIsInstance(classObject.java)
        data.removeAll(items)
    }

    override fun performInsertItem(item: ObjectDataItem<*>): Boolean {
        data.add(item.data)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    fun setNestedProperty(obj: Any, path: String, value: Any?) {
        val parts = path.split(".")

        // Traverser jusqu'à l'avant-dernier segment
        var current: Any = obj
        for (part in parts.dropLast(1)) {
            val prop = current::class.memberProperties.find { it.name == part }
                ?: error("Propriété '$part' introuvable sur ${current::class.simpleName}")
            current = prop.getter.call(current)
                ?: error("Valeur nulle sur le chemin à '$part'")
        }

        // Récupérer la propriété finale et vérifier qu'elle est mutable
        val lastProp = current::class.memberProperties.find { it.name == parts.last() }
            ?: error("Propriété '${parts.last()}' introuvable sur ${current::class.simpleName}")

        val mutableProp = lastProp as? KMutableProperty1<Any, Any?>
            ?: error("La propriété '${parts.last()}' est en val, elle ne peut pas être modifiée")

        mutableProp.setter.call(current, value)
    }


    override fun <T : Any> performItemUpdate(item: ItemUpdate<T>): Boolean {
        val itemToBeUpdated = data
            .filterIsInstance(item.entityType.java)
            .firstOrNull { item.keyName.get(it) == item.keyValue }
            ?: error("Unknown item")

        item.values.forEach { (propName, newValue) ->
            setNestedProperty(itemToBeUpdated, propName, newValue)
        }
        return true
    }

    override fun <T : Any> performItemDelete(item: ItemDelete<T>): Boolean {
        val itemToBeDeleted = data
            .filterIsInstance(item.entityType.java)
            .firstOrNull { item.keyName.get(it) == item.keyValue }
            ?: error("Unknown item")

        val result = data.remove(itemToBeDeleted)
        return result
    }

    override fun commit() {
    }

    override fun close() {
    }

    override fun <T: Any> getObjects(classObject: KClass<T>): List<T> {
        return data.filterIsInstance(classObject.java)
    }

    override fun <T: Any> getSize(classObject: KClass<T>): Long {
        return data.filterIsInstance(classObject.java).size.toLong()
    }
}