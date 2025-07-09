package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ItemError
import org.sbm4j.ktscraping.data.item.ItemStatus
import java.util.concurrent.ConcurrentHashMap


interface ItemReceiver : Controllable {
    var itemIn: ReceiveChannel<Item>

    suspend fun performItems() {
        scope.launch(CoroutineName("${name}-performItems")) {
            logger.debug { "${name}: Waiting for items to process" }
            for (item in itemIn) {
                performItem(item)
            }
        }
    }

    suspend fun performItem(item: Item){
        logger.trace { "${name}: Received an item to process: $item" }
        val resultItem = processItem(item)
        resultItem.forEach {
            pushItem(it)
        }
    }

    override suspend fun run() {
        this.performItems()
    }

    suspend fun processItem(item: Item): List<Item>

    suspend fun pushItem(item: Item)

}

interface ItemForwarder : ItemReceiver, EventConsumer {
    
    var itemOut: SendChannel<Item>

    override suspend fun pushItem(item: Item) {
        itemOut.send(item)
    }

    override suspend fun stop() {
        super.stop()
        this.itemOut.close()
    }


    override suspend fun run() {
        super.run()
        performAcks()
    }

    suspend fun performAck(itemAck: ItemAck): Unit {}

    suspend fun performAcks() {}

}


interface Pipeline : ItemForwarder {

    var itemAckIn: ReceiveChannel<ItemAck>

    var itemAckOut: SendChannel<ItemAck>

    override suspend fun performAcks() {
        scope.launch(CoroutineName("${name}-performAcks")) {
            logger.debug { "${name}: Waiting for items acks to follow" }
            for (itemAck in itemAckIn) {
                logger.trace { "${name}: Received an item ack to process" }
                performAck(itemAck)
                itemAckOut.send(itemAck)
            }
        }
    }

    override suspend fun performAck(itemAck: ItemAck) {
        itemAckOut.send(itemAck)
    }

    fun buildItemError(ex: Exception, level: ErrorLevel = ErrorLevel.MAJOR): ItemError{
        return ItemError(ErrorInfo(ex, this, level))
    }


    override suspend fun performItem(item: Item) {
        try {
            super.performItem(item)
        }
        catch(ex: Exception){
            val errors = listOf(buildItemError(ex))
            val ack = ItemAck(item.itemId, ItemStatus.ERROR, errors)
            itemAckOut.send(ack)
        }
    }


    override suspend fun run() {
        logger.info { "${name}: Starting pipeline" }
        super.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: Stopping pipeline" }
        itemAckOut.close()
        super.stop()
    }

}

abstract class AbstractPipeline(override val name: String) : Pipeline {
    override val mutex: Mutex = Mutex()

    override var state: State = State()

    override lateinit var itemIn: ReceiveChannel<Item>

    override lateinit var itemOut: SendChannel<Item>


    override lateinit var itemAckIn: ReceiveChannel<ItemAck>

    override lateinit var itemAckOut: SendChannel<ItemAck>

    override lateinit var scope: CoroutineScope

    override val pendingEvent: ConcurrentHashMap<String, Job> = ConcurrentHashMap()
}