package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.item.Item


class JSONExporter<T: Data>(name: String = "JSONExporter"): AbstractExporter(name) {
    companion object{
        val json = Json { prettyPrint = true }
    }

    val documents: MutableList<JsonElement> = mutableListOf()

    var out: Channel<String>? = null



    lateinit var serializer: KSerializer<T>

    override suspend fun run() {
        super.run()
    }

    override suspend fun stop() {
        val resultElt = JsonArray(documents)
        val result = json.encodeToString(JsonElement.serializer(), resultElt)
        out?.send(result)
        super.stop()
        documents.removeAll { true }
    }


    override fun exportItem(item: Item) {

        val objectDataItem = item as ObjectDataItem<*>
        val elt = Json.encodeToJsonElement(serializer, objectDataItem.data as T)
        documents.add(elt)
    }
}