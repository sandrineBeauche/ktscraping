package org.sbm4j.ktscraping.core.components

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventSink
import org.sbm4j.ktscraping.core.processors.ItemReceiver
import org.sbm4j.meercat.data.Back
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.logger

abstract class AbstractExporter(
    name: String
): AbstractSinkComponent(name), ItemReceiver, EventSink {

    override suspend fun run() {
        logger.info{"${name}: Starting Exporter"}
        super<EventSink>.run()
        super<ItemReceiver>.run()
        super<AbstractSinkComponent>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping the exporter"}
        super<AbstractSinkComponent>.stop()
    }


    override suspend fun processItem(item: Item): ItemAck {
        logger.debug{ "${name}: exporting the item $item" }
        try {
            exportItem(item)
            logger.debug{"${name}: exported item ${item}, returns ack"}
            return item.buildBack()
        }
        catch(ex: Exception){
            val error = generateErrorInfos(ex)
            return item.buildErrorBack(error)
        }
    }


    override suspend fun sendPostProcess(send: Send, result: Any) {
        logger.trace { "${name}: inside post process -> send ack" }
        inChannel.send(result as Back<*>)
    }

    abstract suspend fun exportItem(item: Item)

}