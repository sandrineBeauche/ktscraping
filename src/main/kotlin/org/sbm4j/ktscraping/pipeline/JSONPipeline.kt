package org.sbm4j.ktscraping.pipeline

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemEnd
import org.sbm4j.ktscraping.requests.ItemStatus
import org.sbm4j.ktscraping.requests.StandardFormatItem
import kotlin.reflect.cast


data class JsonItem(
    override val data: JsonElement
): StandardFormatItem<JsonElement>(data) {
    override fun clone(): Item {
        return this.copy()
    }

    override fun prettyPrint(): String {
        return JSONPipeline.json.encodeToString(data)
    }


}

@OptIn(InternalSerializationApi::class)
fun DataItem<*>.encodeDataToJson(): JsonElement{
    val json = JSONPipeline.json
    return json.encodeToJsonElement(clazz.serializer(), clazz.cast(data))

}

class JSONPipeline(name: String = "JSONPipeline") : AbstractPipeline(name) {
    companion object{
        val json = Json { prettyPrint = true }
    }

    var accumulate: Boolean = false

    lateinit var documents: MutableList<JsonElement>



    override suspend fun run() {
        super.run()
        if(accumulate){
            documents = mutableListOf()
        }
    }

    override suspend fun stop() {
        super.stop()
    }


    @OptIn(InternalSerializationApi::class)
    override suspend fun processItem(item: Item): List<Item> {
        return when (item) {
            is DataItem<*> -> {
                val dataItem = item as DataItem<*>
                val elt = dataItem.encodeDataToJson()

                return if (accumulate) {
                    documents.add(elt)
                    val ack = ItemAck(item.itemId, ItemStatus.PROCESSED)
                    itemAckOut.send(ack)
                    emptyList()
                } else {
                    listOf(JsonItem(elt))
                }
            }

            is ItemEnd -> {
                return if (accumulate) {
                    val json = JsonArray(documents)
                    val elt = JsonItem(json)
                    listOf(elt, item)
                } else {
                    listOf(item)
                }
            }
            else -> listOf(item)
        }

    }
}