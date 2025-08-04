package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

class ChannelExporter(name: String) : AbstractExporter(name) {

    lateinit var channel: Channel<DataItem<*>>

    override suspend fun exportItem(item: DataItem<*>) {
        channel.send(item)
    }
}