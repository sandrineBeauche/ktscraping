package org.sbm4j.ktscraping.data.item

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Serializable
abstract class Data{
    abstract fun clone(): Data
    fun getProperties(): Map<String, Any>{
        return mapOf()
    }
}


data class DataItem<T: Data>(
    val data: T,
    val clazz: KClass<T>,
    val label: String = "data"
): Item(){
    companion object{
        inline fun <reified T: Data> build(data: T, label: String): DataItem<T> {
            return DataItem(data, T::class, label)
        }
    }


    override fun clone(): Item {
        val result = this.copy(data = clazz.cast(data.clone()))
        result.itemId = this.itemId
        return result
    }
}


abstract class StandardFormatItem<T>(
    open val data: T
): Item(){
    abstract fun prettyPrint(): String
}