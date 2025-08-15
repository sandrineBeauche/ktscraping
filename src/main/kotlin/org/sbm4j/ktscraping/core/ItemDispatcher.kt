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
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.Item
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class ItemDispatcher(override val name: String, override val di: DI) : ItemReceiver, Controllable, DIAware {



    lateinit var itemAckOut: SendChannel<AbstractItemAck<*>>

    override val mutex: Mutex = Mutex()

    override var state: State = State()

    val itemOuts: MutableList<SendChannel<Item>> = mutableListOf()

    val itemAckIns: MutableList<ReceiveChannel<AbstractItemAck<*>>> = mutableListOf()

    override lateinit var scope: CoroutineScope

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override suspend fun processDataItem(item: DataItem<*>): List<Item> {
        return listOf(item)
    }


    override suspend fun run() {
        logger.info { "${name}: starting the item dispatcher" }
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

    fun addBranch(itemChannel: Channel<Item>, itemAckChannel: Channel<AbstractItemAck<*>>){
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
                        if(pendingAcks.contains(ack.channelableId)) {
                            pendingAcks[ack.channelableId] = pendingAcks[ack.channelableId] as Int + 1
                            if (pendingAcks[ack.channelableId] == itemAckIns.size) {
                                pendingAcks.remove(ack.channelableId)
                                itemAckOut.send(ack)
                            }
                        }
                        else{
                            logger.warn { "${name}: received an item ack with unknown UUID: ${ack.channelableId}" }
                        }
                    }
                }
            }
        }
    }

    suspend fun pushItem(item: Item) {
        pendingAcks.put(item.channelableId, 0)

        itemOuts.forEach {
            val cl = item.clone()
            it.send(cl)
        }
    }

    override var inChannel: SuperChannel
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        TODO("Not yet implemented")
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
                            var nb = pendingEventAcks[ack.channelableId]
                            if(nb != null){
                                nb++
                                pendingEventAcks[ack.channelableId] = nb
                                if(nb >= itemAckIns.size){
                                    pendingEventAcks.remove(ack.channelableId)
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

    suspend fun pushItem(item: Item) {
        when(item){
            is DataItem<*> -> {
                val channel = selectChannel(item)
                channel.send(item)
            }
            else -> {
                pendingEventAcks[item.channelableId] = 0
                itemOuts.forEach {
                    val cl = item.clone()
                    it.send(cl)
                }
            }
        }

    }
}