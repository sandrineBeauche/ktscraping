package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class ItemDispatcher(override val name: String, override val di: DI) : ItemReceiver, DIAware {

    override lateinit var itemIn: ReceiveChannel<Item>

    lateinit var itemAckOut: SendChannel<AbstractItemAck>

    override val mutex: Mutex = Mutex()

    override var state: State = State()

    val itemOuts: MutableList<SendChannel<Item>> = mutableListOf()

    val itemAckIns: MutableList<ReceiveChannel<AbstractItemAck>> = mutableListOf()

    override lateinit var scope: CoroutineScope

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override suspend fun processDataItem(item: ObjectDataItem<*>): List<Item> {
        return listOf(item)
    }


    override suspend fun run() {
        logger.info { "${name}: starting the item dispatcher" }
        this.performItems()
        this.performAcks()
    }

    override suspend fun stop() {
        logger.info { "${name}: stopping the item dispatcher" }
        itemAckOut.close()
        for(current in itemOuts){
            current.close()
        }
    }


    abstract suspend fun performAcks()

    fun addBranch(itemChannel: Channel<Item>, itemAckChannel: Channel<AbstractItemAck>){
        itemOuts.add(itemChannel)
        itemAckIns.add(itemAckChannel)
    }
}

class ItemDispatcherAll(name: String, di: DI): ItemDispatcher(name, di){

    val pendingAcks: MutableMap<UUID, Int> = mutableMapOf()

    override suspend fun performAcks() {
        for((index, current) in itemAckIns.withIndex()){
            scope.launch(CoroutineName("${name}-performAcks-${index}")) {
                logger.debug { "Waiting for acks" }
                for(ack in current){
                    logger.trace{ "Received the ack ${ack}" }
                    mutex.withLock {
                        if(pendingAcks.contains(ack.itemId)) {
                            pendingAcks[ack.itemId] = pendingAcks[ack.itemId] as Int + 1
                            if (pendingAcks[ack.itemId] == itemAckIns.size) {
                                pendingAcks.remove(ack.itemId)
                                itemAckOut.send(ack)
                            }
                        }
                        else{
                            logger.warn { "${name}: received an item ack with unknown UUID: ${ack.itemId}" }
                        }
                    }
                }
            }
        }
    }

    override suspend fun pushItem(item: Item) {
        pendingAcks.put(item.itemId, 0)

        itemOuts.forEach {
            val cl = item.clone()
            it.send(cl)
        }
    }
}

abstract class ItemDispatcherOne(name: String, di: DI): ItemDispatcher(name, di){

    val pendingEventAcks: MutableMap<UUID, Int> = mutableMapOf()

    abstract fun selectChannel(item: Item): SendChannel<Item>

    override suspend fun performAcks() {
        for(current in itemAckIns){
            scope.launch {
                for(ack in current){
                    logger.trace{"${name}: received item ack: $ack"}
                    when(ack){
                        is EventItemAck -> {
                            var nb = pendingEventAcks[ack.itemId]
                            if(nb != null){
                                nb++
                                pendingEventAcks[ack.itemId] = nb
                                if(nb >= itemAckIns.size){
                                    pendingEventAcks.remove(ack.itemId)
                                    itemAckOut.send(ack)
                                }
                            }
                        }
                        else -> {
                            itemAckOut.send(ack)
                        }
                    }
                }
            }
        }
    }

    override suspend fun pushItem(item: Item) {
        when(item){
            is ObjectDataItem<*> -> {
                val channel = selectChannel(item)
                channel.send(item)
            }
            else -> {
                pendingEventAcks[item.itemId] = 0
                itemOuts.forEach {
                    val cl = item.clone()
                    it.send(cl)
                }
            }
        }

    }
}