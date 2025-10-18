package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.ItemReceiver
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.ktscraping.data.Send

abstract class AbstractExporter(override val name: String): ItemReceiver, EventConsumer, AbstractControllable() {

    override lateinit var inChannel: SuperChannel

    override suspend fun run() {
        logger.info{"${name}: Starting Exporter"}
        super<EventConsumer>.run()
        super<ItemReceiver>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping the exporter"}
        super<EventConsumer>.stop()
        super<ItemReceiver>.stop()
    }


    override suspend fun processItem(item: Item): ItemAck {
        logger.debug{ "${name}: exporting the item $item" }
        try {
            exportItem(item)
            return item.buildBack()
        }
        catch(ex: Exception){
            val error = generateErrorInfos(ex)
            return item.buildErrorBack(error)
        }
    }


    override suspend fun sendPostProcess(send: Send, result: Any) {
        inChannel.send(result as ItemAck)
    }

    abstract suspend fun exportItem(item: Item)

}