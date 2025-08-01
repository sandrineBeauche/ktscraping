package org.sbm4j.ktscraping.pipeline

import kotlinx.coroutines.Job
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


    override suspend fun preEnd(event: Event): EventJobResult? {
        if(accumulate) {
            val json = JsonArray(documents)
            val elt = JsonItem(json)
            itemOut.send(elt)
        }
        return super.preEnd(event)
    }

    @OptIn(InternalSerializationApi::class)
    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        if(item is ObjectDataItem<*>) {
            val elt = item.encodeDataToJson()

            return if (accumulate) {
                documents.add(elt)
                val ack = ItemAck(item.itemId, Status.OK)
                itemAckOut.send(ack)
                emptyList()
            } else {
                listOf(JsonItem(elt))
            }
        }
        else{
            return listOf(item)
        }
    }
}