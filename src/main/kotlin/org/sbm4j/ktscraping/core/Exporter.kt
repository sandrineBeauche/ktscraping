package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemStatus

abstract class AbstractExporter(override val scope: CoroutineScope, override val name: String): ItemReceiver {

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var itemIn: ReceiveChannel<Item>

    lateinit var itemAckOut: SendChannel<ItemAck>

    override suspend fun start() {
        logger.info{"Starting Exporter ${name}"}
        super.start()
    }

    override suspend fun stop() {
        logger.info{"Stopping the exporter ${name}"}
        itemAckOut.close()
        super.stop()
    }

    override fun processItem(item: Item): Item? {
        return item
    }

    abstract fun exportItem(item: Item)

    override suspend fun pushItem(item: Item) {
        logger.debug{ "Exporter ${this.name} exporting the item ${item}" }
        exportItem(item)
        val ack = ItemAck(item.id, ItemStatus.PROCESSED)
        itemAckOut.send(ack)
    }
}