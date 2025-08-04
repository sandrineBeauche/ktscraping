package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.*
import java.util.concurrent.ConcurrentHashMap


interface ItemReceiver : Controllable, EventConsumer {
    var itemIn: ReceiveChannel<Item>

    suspend fun performItems() {
        scope.launch(CoroutineName("${name}-performItems")) {
            logger.debug { "${name}: Waiting for items to process" }
            for (item in itemIn) {
                logger.trace { "${name}: Received an item to process: $item" }
                performItem(item)
            }
        }
    }

    suspend fun performItem(item: Item){
        when(item){
            is EventItem -> performEventItem(item)
            is DataItem<*> -> performDataItem(item)
        }
    }

    suspend fun performEventItem(item: EventItem){
        val result = consumeEvent(item)
        pushItem(result as Item)
    }

    suspend fun performDataItem(item: DataItem<*>){
        val resultItem = processDataItem(item)
        resultItem.forEach {
            pushItem(it)
        }
    }

    override suspend fun run() {
        this.performItems()
    }

    suspend fun processDataItem(item: DataItem<*>): List<Item>{
        return listOf(item)
    }

    suspend fun pushItem(item: Item)

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return ErrorInfo(ex, this, ErrorLevel.MAJOR)
    }
}

interface ItemForwarder : ItemReceiver {
    
    var itemOut: SendChannel<Item>


    override suspend fun pushItem(item: Item) {
        itemOut.send(item)
    }

    override suspend fun stop() {
        this.itemOut.close()
        super.stop()
    }

}


interface ItemSecureForwarder: ItemForwarder{
    var itemAckIn: ReceiveChannel<AbstractItemAck>

    suspend fun performAcks() {
        scope.launch(CoroutineName("${name}-performAcks")) {
            logger.debug { "${name}: Waiting for items acks to follow" }
            for (itemAck in itemAckIn) {
                logger.trace { "${name}: Received an item ack to process" }
                launch(CoroutineName("${name}-performAck-${itemAck.itemId}")){
                    when(itemAck){
                        is EventItemAck -> resumeEvent(itemAck)
                        is DataItemAck -> performDataAck(itemAck)
                    }
                }
            }
        }
    }

    suspend fun performDataAck(itemAck: DataItemAck): Unit {}

    override suspend fun run() {
        performAcks()
        super.run()
    }

}


interface Pipeline : ItemSecureForwarder {

    var itemAckOut: SendChannel<AbstractItemAck>


    override suspend fun performDataAck(itemAck: DataItemAck) {
        itemAckOut.send(itemAck)
    }

    override suspend fun resumeEvent(event: EventBack) {
        super.resumeEvent(event)
        itemAckOut.send(event as EventItemAck)
    }


    override suspend fun performDataItem(item: DataItem<*>) {
        try {
            super.performDataItem(item)
        }
        catch(ex: Exception){
            val infos = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            val errors = mutableListOf(infos)
            val ack = DataItemAck(item.itemId, Status.ERROR, errors)
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


    override lateinit var itemAckIn: ReceiveChannel<AbstractItemAck>

    override lateinit var itemAckOut: SendChannel<AbstractItemAck>

    override lateinit var scope: CoroutineScope

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()
}