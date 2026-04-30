package org.sbm4j.ktscraping.core.processors

import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder

/**
 * A [SendConsumer] that receives [Item] messages in the Pipeline branch.
 *
 * Entry point of the Pipeline branch, [ItemReceiver] processes incoming items
 * extracted by the Spider. The default implementation of [processItem] forwards
 * the item unchanged as a single-element list. Concrete implementations override
 * [processItem] to apply transformations, filtering, validation, or export logic.
 *
 * @see Item
 * @see ItemForwarder
 * @see ItemAckForwarder
 */
interface ItemReceiver : SendConsumer {

    /**
     * Processes an incoming [Item] from the Spider.
     *
     * Defaults to forwarding the item unchanged. Override to apply transformations,
     * filtering, validation, or any pipeline-specific processing.
     *
     * @param item The incoming item to process.
     * @return A list containing the processed item(s), or any other result expected
     * by the pipeline node.
     */
    suspend fun processItem(item: Item): Any?{
        return listOf(item)
    }

    override suspend fun run() {
        val clazz = Item::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::processItem)
    }
}

/**
 * A node that both receives [Item] messages and forwards them downstream
 * in the Pipeline branch.
 *
 * Combines [ItemReceiver] and [SendForwarder] to act as a middleware in the Pipeline
 * branch: it can intercept and process incoming items (e.g. transform, filter, enrich)
 * before forwarding them to the next node.
 *
 * Both [ItemReceiver.run] and [SendForwarder.run] are executed concurrently on startup.
 *
 * @see ItemReceiver
 * @see SendForwarder
 */
interface ItemForwarder: ItemReceiver, SendForwarder {
    override suspend fun run() {
        super<ItemReceiver>.run()
        super<SendForwarder>.run()
    }
}

/**
 * A [BackForwarder] that receives [ItemAck] messages returning from the Pipeline branch
 * and forwards them back toward the Spider.
 *
 * When an [ItemAck] is received from a downstream pipeline node, [processItemAck]
 * reinjects it into [inChannel] to propagate the acknowledgement back up the pipeline
 * chain toward the Spider.
 *
 * @see ItemAck
 * @see ItemReceiver
 */
interface ItemAckForwarder: BackForwarder {

    /**
     * Forwards the [itemAck] back toward the Spider by reinjecting it into [inChannel].
     *
     * @param itemAck The acknowledgement to forward upstream.
     */
    suspend fun processItemAck(itemAck: ItemAck){
        this.inChannel.send(itemAck)
    }

    override suspend fun run() {
        val flow = this.outChannel.getBackFlow(ItemAck::class, this)
        receiveBacks(ItemAck::class, flow, ::processItemAck)
    }
}