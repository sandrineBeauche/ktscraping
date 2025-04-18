package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.ItemStatus

abstract class AbstractExporter(override val name: String): ItemReceiver {

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var scope: CoroutineScope

    override lateinit var itemIn: ReceiveChannel<Item>

    lateinit var itemAckOut: SendChannel<ItemAck>

    override suspend fun run() {
        logger.info{"${name}: Starting Exporter"}
        super.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping the exporter"}
        itemAckOut.close()
        super.stop()
    }

    override suspend fun processItem(item: Item): List<Item> {
        return listOf(item)
    }

    abstract fun exportItem(item: Item)

    override suspend fun pushItem(item: Item) {
        logger.debug{ "${name}: exporting the item ${item}" }
        lateinit var ack: ItemAck
        try {
            exportItem(item)
            ack = ItemAck(item.itemId, ItemStatus.PROCESSED)
        }
        catch(ex: Exception){
            ack = ItemAck(item.itemId, ItemStatus.ERROR)
        }
        finally {
            itemAckOut.send(ack)
        }
    }
}