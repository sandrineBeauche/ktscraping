package org.sbm4j.ktscraping.data.item

import kotlinx.serialization.Serializable
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Base class for scraped data models.
 *
 * Users of KtScraping extend this class to define their own domain-specific data models
 * (e.g. `class Product : Data()`, `class Article : Data()`). The [getProperties] method
 * can be overridden to expose the model's fields as a key-value map, for example for
 * serialization or export purposes.
 *
 * [Data] is [Serializable] to support persistence and inter-process transfer.
 *
 * @see DataItem
 * @see ObjectDataItem
 */
@Serializable
abstract class Data{
    /** Creates a copy of this data object. Must be implemented by each subclass. */
    abstract fun clone(): Data

    /**
     * Returns the fields of this data object as a key-value map.
     *
     * Defaults to an empty map. Subclasses may override this to expose their fields,
     * for example for CSV export or generic pipeline processing.
     *
     * @return A map of field names to their values.
     */
    fun getProperties(): Map<String, Any>{
        return mapOf()
    }

}

/**
 * An [Item] carrying a typed data payload of type [T].
 *
 * Base class for all data-bearing items in the Pipeline branch.
 * Concrete subclasses provide the actual [data] value and define
 * how it is structured (domain object, standard format, etc.).
 *
 * @param T The type of data carried by this item.
 *
 * @see ObjectDataItem
 * @see StandardFormatItem
 */
abstract class DataItem<T>() : Item(){
    /** The data payload carried by this item. */
    abstract val data: T
}

/**
 * An [Item] carrying a domain-specific [Data] object through the Pipeline branch.
 *
 * Wraps a user-defined [Data] subclass instance (e.g. `Product`, `Article`) along with
 * its [KClass] reference to enable safe cloning and casting. The [label] can be used
 * by pipeline nodes to identify or categorize the item.
 *
 * @property data The domain data object carried by this item.
 * @property clazz The [KClass] of [T], used for safe casting during [clone].
 * @property label A descriptive label for this item, defaults to `"data"`.
 * @property sender The Spider emitting this item.
 *
 * @see Data
 * @see DataItem
 */
data class ObjectDataItem<T: Data>(
    override val data: T,
    val clazz: KClass<T>,
    val label: String = "data",
    override var sender: SendSource
): DataItem<T>(){
    companion object{
        /**
         * Factory method to build an [ObjectDataItem] without explicitly passing the [KClass].
         *
         * Uses a reified type parameter to automatically capture `T::class`.
         *
         * @param T The [Data] subclass type, reified.
         * @param data The data object to wrap.
         * @param label A descriptive label for this item.
         * @param sender The Spider emitting this item.
         * @return A new [ObjectDataItem] wrapping [data].
         */
        inline fun <reified T: Data> build(data: T, label: String, sender: SendSource): ObjectDataItem<T> {
            return ObjectDataItem(data, T::class, label, sender)
        }
    }

    /** Technical name of this item, based on the simple class name of [T]. */
    override val name: String = "ObjectDataItem-${clazz.simpleName}"

    /**
     * Creates a deep copy of this item by cloning the [data] object and preserving
     * the [channelableId] so the copy is recognized as the same message in the topology.
     */
    override fun clone(): Item {
        val result = this.copy(data = clazz.cast(data.clone()))
        result.channelableId = this.channelableId
        return result
    }

    /**
     * Returns [name] as the barrier key, allowing [Barrier] nodes to synchronize
     * branches on the item type.
     */
    override fun getKeyBarrier(): String {
        return this.name
    }
}

/**
 * An [Item] carrying data in a standard serialization format (e.g. CSV, JSON).
 *
 * Provides a [prettyPrint] method for human-readable output of the formatted data.
 * Use this as a base for items that represent data in a predefined interchange format,
 * as opposed to [ObjectDataItem] which wraps user-defined domain objects.
 *
 * @param T The standard format type (e.g. a JSON string, a CSV row representation).
 * @param data The formatted data payload.
 *
 * @see DataItem
 * @see ObjectDataItem
 */
abstract class StandardFormatItem<T>(
    override val data: T
): DataItem<T>(){
    /**
     * Returns a human-readable string representation of the formatted data.
     *
     * @return A pretty-printed string of the data payload.
     */
    abstract fun prettyPrint(): String
}