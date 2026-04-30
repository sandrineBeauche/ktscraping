package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.ItemReceiver
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.logger

/**
 * Base abstract class for Pipeline branch sink nodes that export scraped [Item]s.
 *
 * An [AbstractExporter] is the terminal node of the Pipeline branch. It receives
 * [Item] messages from the Spider via [ItemReceiver], delegates the actual export
 * to [exportItem], and returns an [ItemAck] reflecting the outcome.
 *
 * Concrete implementations override [exportItem] to write data to a target destination
 * (e.g. database, file, API, message queue, etc.).
 *
 * @param name The name of this exporter node.
 *
 * @see ItemReceiver
 * @see AbstractSinkComponent
 * @see Item
 * @see ItemAck
 */
abstract class AbstractExporter(
    name: String
): AbstractSinkComponent(name), ItemReceiver {

    override suspend fun run() {
        logger.info{"${name}: Starting Exporter"}
        super<ItemReceiver>.run()
        super<AbstractSinkComponent>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping the exporter"}
        super<AbstractSinkComponent>.stop()
    }

    /**
     * Processes an incoming [Item] by delegating to [exportItem] and returning
     * an [ItemAck] reflecting the outcome.
     *
     * Any exception thrown by [exportItem] is caught and reflected in the returned
     * [ItemAck] as an error back.
     *
     * @param item The item to export.
     * @return An [ItemAck] with [Status.OK] on success, or an error back on failure.
     */
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

    /**
     * Exports the given [item] to the target destination.
     *
     * Implement this method to write the item's data to a database, file, API,
     * message queue, or any other export target.
     *
     * @param item The item to export.
     * @throws Exception if the export fails. The exception will be caught by
     * [processItem] and reflected in the returned [ItemAck].
     */
    abstract suspend fun exportItem(item: Item)
}