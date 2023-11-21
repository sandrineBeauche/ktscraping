package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

interface ResponseDispatcher: Controllable, DIAware {

    val senders : MutableMap<ReceiveChannel<Request>, SendChannel<Response>>

    val channelOut: SendChannel<Request>

    val channelIn: ReceiveChannel<Response>

    val pendingRequests: PendingRequestMap


    suspend fun performRequests(){
        for ((receiver, sender) in senders) {
            scope.launch {
                for(request in receiver) {
                    logger.debug { "Received request ${request.name} and follows it" }
                    pendingRequests[request.reqId] = sender as Channel<Response>
                    channelOut.send(request)
                }
            }

        }
    }


    suspend fun performResponses(){
        scope.launch {
            for(response in channelIn){
                val req = response.request
                logger.debug{ "Received response for the request ${req.name} and dispatch it"}
                val channel = pendingRequests.remove(req.reqId)
                channel?.send(response)
            }
        }
    }


    override suspend fun start() {
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
    override val scope: CoroutineScope,
    override val name: String = "SpiderResponseDispatcher",
    override val di: DI
): ResponseDispatcher{

    override val senders: MutableMap<ReceiveChannel<Request>, SendChannel<Response>> = mutableMapOf()

    val itemSenders: MutableList<ReceiveChannel<Item>> = mutableListOf()

    override lateinit var channelOut: SendChannel<Request>

    override lateinit var channelIn: ReceiveChannel<Response>

    lateinit var itemChannelOut: SendChannel<Item>

    override val pendingRequests: PendingRequestMap = PendingRequestMap()
    override val mutex: Mutex = Mutex()

    override var state: State = State()


    suspend fun performItems(){
        for(sender in itemSenders){
            scope.launch {
                for(item in sender){
                    logger.debug{ "Received an item and follows it" }
                    itemChannelOut.send(item)
                }
            }
        }
    }

    override suspend fun start() {
        this.performItems()
        super.start()
    }

    override suspend fun stop() {
        this.itemChannelOut.close()
        super.stop()
    }

    fun addBranch(reqChannel: Channel<Request>, respChannel: Channel<Response>, itemChannel: Channel<Item>){
        this.senders.put(reqChannel, respChannel)
        this.itemSenders.add(itemChannel)
    }
}