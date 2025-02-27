package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.requests.*

interface ResponseDispatcher: Controllable, DIAware {

    val senders : MutableMap<ReceiveChannel<AbstractRequest>, SendChannel<Response>>

    val channelOut: SendChannel<AbstractRequest>

    val channelIn: ReceiveChannel<Response>

    val pendingRequests: PendingRequestMap


    suspend fun performRequests(){
        for ((index, entry) in senders.entries.withIndex()) {
            val (receiver, sender) = entry
            scope.launch(CoroutineName("${name}-performRequests-${index}")) {
                for(request in receiver) {
                    logger.debug { "Received request ${request.name} and follows it" }
                    pendingRequests[request.reqId] = sender as Channel<Response>
                    channelOut.send(request)
                }
            }
        }
    }


    suspend fun performResponses(){
        scope.launch(CoroutineName("${name}-performResponses")) {
            for(response in channelIn){
                val req = response.request
                logger.debug{ "Received response for the request ${req.name} and dispatch it"}
                val channel = pendingRequests.remove(req.reqId)
                channel?.send(response)
            }
        }
    }


    override suspend fun run() {
        logger.info{ "Starting the response dispatcher ${name}"}
        this.performRequests()
        this.performResponses()
    }

    override suspend fun stop() {
        logger.info{ "Stopping the response dispatcher ${name}"}
        this.channelOut.close()
        for(sender in senders.values){
            sender.close()
        }
    }
}

class SpiderResponseDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    override val di: DI
): ResponseDispatcher{

    override val senders: MutableMap<ReceiveChannel<AbstractRequest>, SendChannel<Response>> = mutableMapOf()

    val itemSenders: MutableList<ReceiveChannel<Item>> = mutableListOf()

    override lateinit var channelOut: SendChannel<AbstractRequest>

    override lateinit var channelIn: ReceiveChannel<Response>

    lateinit var itemChannelOut: SendChannel<Item>

    override val pendingRequests: PendingRequestMap = PendingRequestMap()
    override val mutex: Mutex = Mutex()

    override var state: State = State()

    var nbItemEnd: Int = 0

    override lateinit var scope: CoroutineScope

    suspend fun performItems(){
        for(sender in itemSenders){
            scope.launch(CoroutineName("${name}-performItems")) {
                for(item in sender){
                    performItem(item)
                }
            }
        }
    }

    suspend fun performItem(item: Item){
        if(item is ItemEnd){
            logger.debug { "${name}: received an item end" }
            nbItemEnd++
            if(nbItemEnd >= itemSenders.size){
                logger.debug { "${name}: All branches are finished, follows item end "}
                itemChannelOut.send(item)
            }
            else{
                logger.debug { "${name}: Wait for others item ends"}
            }
        }
        else{
            logger.debug{ "${name}: Received an item and follows it: ${item}" }
            itemChannelOut.send(item)
        }
    }


    override suspend fun run() {
        this.performItems()
        super.run()
    }

    override suspend fun stop() {
        this.itemChannelOut.close()
        super.stop()
    }

    fun addBranch(reqChannel: Channel<AbstractRequest>, respChannel: Channel<Response>, itemChannel: Channel<Item>){
        this.senders.put(reqChannel, respChannel)
        this.itemSenders.add(itemChannel)
    }
}