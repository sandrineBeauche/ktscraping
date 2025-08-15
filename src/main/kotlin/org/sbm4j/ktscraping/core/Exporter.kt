package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.DataItemAck
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractExporter(override val name: String): SendConsumer<Item>, ItemReceiver, Controllable {

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override lateinit var scope: CoroutineScope



    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override suspend fun run() {
        logger.info{"${name}: Starting Exporter"}
        //this.performSends()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping the exporter"}

    }

    override suspend fun sendPostProcess(send: Item, result: Any) {
        when(send){
            is DataItem<*> -> pushDataItem(send)
            is EventItem -> pushEventItem(send)
        }
    }

    abstract suspend fun exportItem(item: DataItem<*>)


    suspend fun pushDataItem(item: DataItem<*>){
        logger.debug{ "${name}: exporting the item ${item}" }
        lateinit var ack: DataItemAck
        try {
            exportItem(item)
            ack = DataItemAck(item, Status.OK)
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            ack = DataItemAck(item, Status.ERROR, mutableListOf(error))
        }
        finally {
            inChannel.send(ack)
        }
    }

    suspend fun pushEventItem(item: EventItem){
        lateinit var ack: EventItemAck
        try {
            consumeEvent(item)
            ack = EventItemAck(item, Status.OK)
        }
        catch(ex: Exception){
            val error = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            ack = EventItemAck(item,
                Status.ERROR, mutableListOf(error))
        }
        finally {
            inChannel.send(ack)
        }
    }
}