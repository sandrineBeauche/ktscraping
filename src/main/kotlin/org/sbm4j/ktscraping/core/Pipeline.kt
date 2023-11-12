package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.requests.Item


interface ItemReceiver: Controllable{
    val itemIn: ReceiveChannel<Item>

    suspend fun performItems(){
        scope.launch{
            logger.debug { "Waiting for items to process" }
            for(item in itemIn){
                logger.debug{ "Received an item to process" }
                var result = false
                mutex.withLock {
                    result = processItem(item)
                }
                if(result){
                    pushItem(item)
                }
            }
        }
    }

    fun processItem(item: Item): Boolean

    suspend fun pushItem(item: Item)

}


interface Pipeline : ItemReceiver {
    val itemOut: SendChannel<Item>

    override suspend fun start() {
        this.performItems()
    }

    override suspend fun stop() {
        this.itemOut.close()
    }

    override suspend fun pushItem(item: Item) {
        itemOut.send(item)
    }
}

abstract class AbstractPipeline(): Pipeline{
    override val mutex: Mutex
        get() = TODO("Not yet implemented")
    override val name: String
        get() = TODO("Not yet implemented")
    override var state: State
        get() = TODO("Not yet implemented")
        set(value) {}

    override val itemIn: ReceiveChannel<Item>
        get() = TODO("Not yet implemented")

    override fun processItem(item: Item): Boolean {
        TODO("Not yet implemented")
    }

    override val itemOut: SendChannel<Item>
        get() = TODO("Not yet implemented")
}