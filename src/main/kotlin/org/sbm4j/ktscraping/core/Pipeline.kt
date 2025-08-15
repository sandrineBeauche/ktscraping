package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.item.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


interface ItemReceiver : EventConsumer {

    var inChannel: SuperChannel

    suspend fun processSend(send: Item): Any? {
        return when(send){
            is EventItem -> consumeEvent(send)
            is DataItem<*> -> processDataItem(send)
            else -> listOf(send)
        }
    }

    suspend fun processDataItem(item: DataItem<*>): List<Item>{
        return listOf(item)
    }
}



interface ItemForwarder: ItemReceiver, SendForwarder<Item>{

    override suspend fun performBack(back: Back<Item>) {
        logger.trace { "${name}: Received an item ack to process" }
        scope.launch(CoroutineName("${name}-performAck-${back.channelableId}")){
            when(back){
                is EventBack -> resumeEvent(back)
                is DataItemAck -> performDataAck(back)
            }
        }
    }

    suspend fun performDataAck(itemAck: DataItemAck): Unit {
        inChannel.send(itemAck)
    }

    override suspend fun resumeEvent(event: EventBack<*>) {
        super.resumeEvent(event)
        inChannel.send(event)
    }

    override suspend fun processSend(send: Item): Any? {
        return super.processSend(send)
    }
}

interface Pipeline : ItemForwarder {

    override suspend fun performBack(back: Back<Item>) {
        super.performBack(back)
    }

    override suspend fun performDataAck(itemAck: DataItemAck) {
        inChannel.send(itemAck)
    }



    override suspend fun processSend(send: Item): Any? {
        return super.processSend(send)
    }


    override suspend fun sendPostProcess(send: Item, result: Any) {

    }


    override suspend fun run() {
        logger.info { "${name}: Starting pipeline" }
    }

    override suspend fun stop() {
        logger.info { "${name}: Stopping pipeline" }
    }

}

abstract class AbstractPipeline(override var name: String) : Pipeline, Controllable {
    override val mutex: Mutex = Mutex()

    override var state: State = State()

    override lateinit var scope: CoroutineScope

    override lateinit var inChannel: SuperChannel

    override lateinit var outChannel: SuperChannel

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override val pendingMinorError: ConcurrentHashMap<UUID, MutableList<ErrorInfo>> = ConcurrentHashMap()

    override val sendClazz: KClass<*> = Item::class

    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return ErrorInfo(ex, this, ErrorLevel.MAJOR)
    }
}