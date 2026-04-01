package org.sbm4j.ktscraping.core.processors

import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder

interface ItemReceiver : SendConsumer {

    suspend fun processItem(item: Item): Any?{
        return listOf(item)
    }

    override suspend fun run() {
        val clazz = Item::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::processItem)
    }
}

interface ItemForwarder: ItemReceiver, SendForwarder {
    override suspend fun run() {
        super<ItemReceiver>.run()
        super<SendForwarder>.run()
    }
}

interface ItemAckForwarder: BackForwarder {

    suspend fun processItemAck(itemAck: ItemAck){
        this@ItemAckForwarder.inChannel.send(itemAck)
    }

    override suspend fun run() {
        val flow = this@ItemAckForwarder.outChannel.getBackFlow(ItemAck::class, this)
        receiveBacks(ItemAck::class, flow, ::processItemAck)
    }
}