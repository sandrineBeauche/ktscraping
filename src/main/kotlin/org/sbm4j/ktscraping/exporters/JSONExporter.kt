package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item

class JSONExporter(name: String = "JSONExporter"): AbstractExporter(name) {

    val documents: MutableList<DataItem> = mutableListOf()


    override suspend fun run() {
        super.run()
    }

    override suspend fun stop() {
        val result = Json.encodeToString(documents)
        //jsonFile.writeText(result)
        super.stop()
        documents.removeAll { true }
    }

    override fun exportItem(item: Item) {
        documents.add(item as DataItem)
    }
}