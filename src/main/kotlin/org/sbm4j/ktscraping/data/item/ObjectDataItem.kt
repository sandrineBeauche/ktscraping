package org.sbm4j.ktscraping.data.item

import kotlinx.serialization.Serializable
import org.sbm4j.ktscraping.core.components.Controllable
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Serializable
abstract class Data{
    abstract fun clone(): Data
    fun getProperties(): Map<String, Any>{
        return mapOf()
    }

}


abstract class DataItem<T>() : Item(){
    abstract val data: T
}

data class ObjectDataItem<T: Data>(
    override val data: T,
    val clazz: KClass<T>,
    val label: String = "data",
    override var sender: Controllable
): DataItem<T>(){
    companion object{
        inline fun <reified T: Data> build(data: T, label: String, sender: Controllable): ObjectDataItem<T> {
            return ObjectDataItem(data, T::class, label, sender)
        }
    }

    override val name: String = "ObjectDataItem"

    override fun clone(): Item {
        val result = this.copy(data = clazz.cast(data.clone()))
        result.channelableId = this.channelableId
        return result
    }

}


abstract class StandardFormatItem<T>(
    override val data: T
): DataItem<T>(){
    abstract fun prettyPrint(): String
}