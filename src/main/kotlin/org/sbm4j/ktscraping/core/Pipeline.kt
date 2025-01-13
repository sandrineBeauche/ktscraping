package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck


interface ItemReceiver: Controllable{
    var itemIn: ReceiveChannel<Item>


    suspend fun performItems(){
        scope.launch(CoroutineName("${name}-performItems")){
            logger.debug { "${name}: Waiting for items to process" }
            for(item in itemIn){
                logger.debug{ "${name}: Received an item to process" }
                //mutex.withLock {
                    val resultItem = processItem(item)
                    resultItem.forEach {
                        pushItem(it)
                    }
                //}

            }
        }
    }

    override suspend fun start() {
        this.performItems()
    }

    fun processItem(item: Item): List<Item>

    suspend fun pushItem(item: Item)

}

interface ItemFollower: ItemReceiver{


    var itemOut: SendChannel<Item>

    override suspend fun pushItem(item: Item) {
        itemOut.send(item)
    }

    override suspend fun stop() {
        super.stop()
        this.itemOut.close()

    }

    override suspend fun start() {
        super.start()
        performAcks()
    }

    suspend fun performAck(itemAck: ItemAck): Unit{}

    suspend fun performAcks(){}

}


interface Pipeline : ItemFollower {

    var itemAckIn: ReceiveChannel<ItemAck>

    var itemAckOut: SendChannel<ItemAck>

    override suspend fun performAcks(){
        scope.launch(CoroutineName("${name}-performAcks")){
            logger.debug { "${name}: Waiting for items acks to follow" }
            for(itemAck in itemAckIn){
                logger.debug{ "${name}: Received an item ack to process" }
                performAck(itemAck)
                itemAckOut.send(itemAck)
            }
        }
    }

    override suspend fun start() {
        logger.info{"${name}: Starting pipeline"}
        this.performAcks()
        super.start()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping pipeline"}
        itemAckOut.close()
        super.stop()
    }

}

abstract class AbstractPipeline(override val scope: CoroutineScope, override val name: String): Pipeline{
    override val mutex: Mutex = Mutex()

    override var state: State = State()

    override lateinit var itemIn: ReceiveChannel<Item>

    override lateinit var itemOut: SendChannel<Item>


    override lateinit var itemAckIn: ReceiveChannel<ItemAck>

    override lateinit var itemAckOut: SendChannel<ItemAck>

}