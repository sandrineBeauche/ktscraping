package org.sbm4j.ktscraping.exporters

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.data.item.Item

class ChannelExporter(name: String) : AbstractExporter(name) {

    lateinit var channel: Channel<Item>

    override suspend fun exportItem(item: Item) {
        channel.send(item)
    }
}