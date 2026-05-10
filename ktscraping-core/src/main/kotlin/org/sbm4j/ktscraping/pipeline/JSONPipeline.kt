package org.sbm4j.ktscraping.pipeline

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.StandardFormatItem
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.reflect.cast


data class JsonItem(
    override val data: JsonElement,
    override var sender: SendSource,
    override val name: String
): StandardFormatItem<JsonElement>(data) {
    override fun clone(): Item {
        return this.copy()
    }

    override fun prettyPrint(): String {
        return JSONPipeline.json.encodeToString(data)
    }

    override fun getKeyBarrier(): String {
        return name
    }
}

@OptIn(InternalSerializationApi::class)
fun ObjectDataItem<*>.encodeDataToJson(): JsonElement{
    val json = JSONPipeline.json
    return json.encodeToJsonElement(clazz.serializer(), clazz.cast(data))

}

class JSONPipeline(name: String = "JSONPipeline") : AbstractPipeline(name) {
    companion object{
        val json = Json { prettyPrint = true }
    }


    @OptIn(InternalSerializationApi::class)
    override suspend fun processItem(item: Item): List<Item> {
        return if(item is ObjectDataItem<*>) {
            val elt = item.encodeDataToJson()
            listOf(JsonItem(elt, this, "json"))
        }
        else{
            listOf(item)
        }
    }
}

class AccumulateJSONPipeline(name: String = "AccumulateJSONPipeline") : AggregatePipeline(name) {

    val documents: MutableList<JsonElement> = mutableListOf()

    override fun accumulateItem(item: Item) {
        if(item is ObjectDataItem<*>){
            val elt = item.encodeDataToJson()
            documents.add(elt)
        }
    }

    override fun aggregate(): List<Item> {
        val json = JsonArray(documents)
        val elt = JsonItem(json, this, "json")
        return listOf(elt)
    }
}