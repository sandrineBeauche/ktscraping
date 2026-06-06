package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.meercat.nodes.logger

/**
 * Mixin interface for [Component] nodes that interact with a [DBConnexion].
 *
 * Provides a unified dispatch mechanism ([performDBItem]) and default implementations
 * for insert, update, and delete operations, delegating to the underlying [db] connection.
 * Multiple components sharing the same [DBConnexion] instance can all implement this
 * interface without duplicating database interaction logic.
 *
 * The [db] connection lifecycle (open/close) is managed externally via the
 * [EventProcessor.preStart]/[EventProcessor.preEnd] hooks of the implementing component.
 *
 * @see DBConnexion
 * @see AbstractDBExporter
 * @see DBSyncMiddleware
 */
interface DBControllable {

    /** The shared database connection used by this component. */
    var db: DBConnexion

    /** The name of the implementing component, used for logging. */
    val name: String

    /**
     * Dispatches an [Item] to the appropriate database operation based on its type:
     * - [ObjectDataItem] → [perfomInsertItem]
     * - [ItemUpdate] → [performItemUpdate]
     * - [ItemDelete] → [performItemDelete]
     *
     * @param item The item to persist.
     */
    fun performDBItem(item: Item): Boolean{
        return when(item){
            is ItemUpdate<*> -> {
                logger.debug { "${name}: update item ${item}" }
                performItemUpdate(item)
            }
            is ItemDelete<*> -> {
                logger.debug { "${name}: delete item ${item}"}
                performItemDelete(item)
            }
            is ObjectDataItem<*> -> {
                logger.debug { "${name}: insert item ${item}"}
                perfomInsertItem(item)
            }
            else -> false
        }
    }

    /**
     * Applies a partial update via [DBConnexion.performItemUpdate].
     * Override to customize update behavior.
     *
     * @param item The partial update to apply.
     */
    fun performItemUpdate(item: ItemUpdate<*>): Boolean{
        return db.performItemUpdate(item)
    }

    /**
     * Applies a delete operation via [DBConnexion.performItemDelete].
     * Override to customize delete behavior.
     *
     * @param item The delete operation to apply.
     */
    fun performItemDelete(item: ItemDelete<*>): Boolean{
        return db.performItemDelete(item)
    }

    /**
     * Inserts a new record via [DBConnexion.performInsertItem].
     * Override to customize insert behavior.
     *
     * @param item The item to insert.
     */
    fun perfomInsertItem(item: ObjectDataItem<*>): Boolean{
        return db.performInsertItem(item)
    }
}