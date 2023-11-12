package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

abstract class AbstractEngine(
    override val scope: CoroutineScope,
    channelFactory: ChannelFactory,
) : RequestSender, RequestReceiver, Pipeline{

    override val mutex: Mutex = Mutex()
    override var state = State()

    override val name: String = "Engine"

    override val itemIn: ReceiveChannel<Item> = channelFactory.spiderItemChannel
    override val itemOut: SendChannel<Item> = channelFactory.itemChannel

    override val requestIn: ReceiveChannel<Request> = channelFactory.spiderRequestChannel
    override val responseOut: SendChannel<Response> = channelFactory.spiderResponseChannel

    override val requestOut: SendChannel<Request> = channelFactory.downloaderRequestChannel
    override val responseIn: ReceiveChannel<Response> = channelFactory.downloaderResponseChannel

    override var pendingRequests: PendingRequestMap = PendingRequestMap()

    override fun processItem(item: Item): Boolean {
        return true
    }


    override fun processRequest(request: Request): Any? {
        return request
    }

    override suspend fun performResponse(response: Response) {
        this.responseOut.send(response)
    }

    override suspend fun start() {
        super<RequestSender>.start()
        super<RequestReceiver>.start()
        super<Pipeline>.start()
    }

    override suspend fun stop() {
        super<RequestSender>.stop()
        super<RequestReceiver>.stop()
        super<Pipeline>.stop()
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

    override suspend fun answerRequest(request: Request, result: Any?) {
        this.scheduler.submitRequest(request)
    }

}