package org.sbm4j.ktscraping.pipeline

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.sbm4j.ktscraping.core.AbstractPipeline
import org.sbm4j.ktscraping.core.EventJobResult
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
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
fun ObjectDataItem<*>.encodeDataToJson(): JsonElement{
    val json = JSONPipeline.json
    return json.encodeToJsonElement(clazz.serializer(), clazz.cast(data))

}

class JSONPipeline(name: String = "JSONPipeline") : AbstractPipeline(name) {
    companion object{
        val json = Json { prettyPrint = true }
    }


    @OptIn(InternalSerializationApi::class)
    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        return if(item is ObjectDataItem<*>) {
            val elt = item.encodeDataToJson()
            listOf(JsonItem(elt))
        }
        else{
            listOf(item)
        }
    }
}

class AccumulateJSONPipeline(name: String = "AccumulateJSONPipeline") : AccumulatePipeline(name) {

    val documents: MutableList<JsonElement> = mutableListOf()

    override fun accumulateItem(item: DataItem<*>) {
        if(item is ObjectDataItem){
            val elt = item.encodeDataToJson()
            documents.add(elt)
        }
    }

    override fun generateItems(): List<Item> {
        val json = JsonArray(documents)
        val elt = JsonItem(json)
        return listOf(elt)
    }
}