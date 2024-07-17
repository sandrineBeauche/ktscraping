package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

abstract class AbstractEngine(
    final override val scope: CoroutineScope,
    channelFactory: ChannelFactory,
) : RequestSender, RequestReceiver, ItemFollower{

    override val mutex: Mutex = Mutex()
    override var state = State()

    override val name: String = "Engine"

    override var itemIn: ReceiveChannel<Item> = channelFactory.spiderItemChannel
    override var itemOut: SendChannel<Item> = channelFactory.itemChannel

    var itemAckIn: ReceiveChannel<ItemAck> = channelFactory.itemAckChannel

    override var requestIn: ReceiveChannel<Request> = channelFactory.spiderRequestChannel
    override val responseOut: SendChannel<Response> = channelFactory.spiderResponseChannel

    override val requestOut: SendChannel<Request> = channelFactory.downloaderRequestChannel
    override val responseIn: ReceiveChannel<Response> = channelFactory.downloaderResponseChannel

    override var pendingRequests: PendingRequestMap = PendingRequestMap()

    override fun processItem(item: Item): Item? {
        return item
    }


    override suspend fun performResponse(response: Response) {
        this.responseOut.send(response)
    }

    override suspend fun start() {
        super<RequestSender>.start()
        super<RequestReceiver>.start()
        super<ItemFollower>.start()
    }

    override suspend fun stop() {
        super<RequestSender>.stop()
        super<RequestReceiver>.stop()
        super<ItemFollower>.stop()
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun resume() {
        TODO("Not yet implemented")
    }
}

class Engine(
    scope: CoroutineScope,
    channelFactory: ChannelFactory,
    val scheduler: Scheduler
) : AbstractEngine(scope, channelFactory){

    override fun processRequest(request: Request): Any? {
        return true
    }

    override suspend fun answerRequest(request: Request, result: Any?) {
        this.scheduler.submitRequest(request)
    }

    override suspend fun performResponse(response: Response) {
        this.scheduler.receivedResponse()
        super.performResponse(response)
    }

    override suspend fun start() {
        super.start()
        scheduler.start()
    }

    override suspend fun stop() {
        scheduler.stop()
        super.stop()
    }
}