package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.requests.DataItem
import org.sbm4j.ktscraping.requests.Item

class JSONExporter(scope: CoroutineScope, name: String = "JSONExporter"): AbstractExporter(scope, name) {

    val documents: MutableList<DataItem> = mutableListOf()

    override suspend fun start() {
        super.start()
        documents.removeAll { true }
    }

    override suspend fun stop() {
        val result = Json.encodeToString(documents)
        //jsonFile.writeText(result)
        super.stop()
    }

    override fun exportItem(item: Item) {
        documents.add(item as DataItem)
    }
}