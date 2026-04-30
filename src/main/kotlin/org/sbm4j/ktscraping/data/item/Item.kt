package org.sbm4j.ktscraping.data.item

import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Status

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for data items extracted by a Spider and sent to the Pipeline branch for processing.
 *
 * An [Item] represents a single piece of scraped data (e.g. a product, an article, a record)
 * that travels through the Pipeline branch to be transformed, filtered, or exported
 * by pipeline nodes.
 *
 * Concrete subclasses define the actual data fields relevant to the scraping use case.
 *
 * @see ItemAck
 */
abstract class Item : Send {
    companion object {
        /** Global atomic counter used to generate unique item identifiers. */
        val lastId = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    /** Creates a copy of this item. Must be implemented by each subclass. */
    abstract override fun clone(): Item

    /**
     * Builds an error back for this item.
     *
     * @param infos Details of the error that occurred during pipeline processing.
     * @param status Error status to apply.
     * @return An [ItemAck] with the provided error info.
     */
    override fun buildErrorBack(infos: ErrorInfo, status: Status): ItemAck {
        return ItemAck(this, Status.ERROR, mutableListOf(infos))
    }

    /**
     * Builds a nominal back for this item.
     *
     * @return An [ItemAck] with [Status.OK].
     */
    override fun buildBack(): ItemAck {
        return ItemAck(this)
    }

}
