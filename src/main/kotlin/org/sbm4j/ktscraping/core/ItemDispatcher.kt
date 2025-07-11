package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import java.util.*

abstract class ItemDispatcher(override val name: String, override val di: DI) : ItemReceiver, DIAware {

    override lateinit var itemIn: ReceiveChannel<Item>

    lateinit var itemAckOut: SendChannel<ItemAck>

    override val mutex: Mutex = Mutex()

    override var state: State = State()

    val itemOuts: MutableList<SendChannel<Item>> = mutableListOf()

    val itemAckIns: MutableList<ReceiveChannel<ItemAck>> = mutableListOf()

    override lateinit var scope: CoroutineScope

    override suspend fun processItem(item: Item): List<Item> {
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

    fun addBranch(itemChannel: Channel<Item>, itemAckChannel: Channel<ItemAck>){
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
                        pendingAcks[ack.itemId] = pendingAcks[ack.itemId] as Int + 1
                        if(pendingAcks[ack.itemId] == itemAckIns.size){
                            pendingAcks.remove(ack.itemId)
                            itemAckOut.send(ack)
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

    abstract fun selectChannel(item: Item): SendChannel<Item>

    override suspend fun performAcks() {
        for(current in itemAckIns){
            scope.launch {
                for(ack in current){
                    itemAckOut.send(ack)
                }
            }
        }
    }

    override suspend fun pushItem(item: Item) {
        val channel = selectChannel(item)
        channel.send(item)
    }
}