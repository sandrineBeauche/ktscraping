package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

/**
 * A piece of the kt scraping line to perform some operations on requests, and answer by
 * following them to the next piece. In the other side, Middleware processes responses
 * before following them to the previous piece.
 */
interface Middleware : RequestSender, RequestReceiver {

    /**
     * Answers a request by following it to the next piece or return a response to the sender
     */
    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        if(result is Response){
            logger.trace { "${name}: returns a response for the request ${request.name}" }
            responseOut.send(result)
        }
        else {
            logger.trace { "${name}: follows request ${request.name}" }
            requestOut.send(request)
        }
    }

    override suspend fun performResponse(response: Response) {

        if(response.request.sender != this){
            val result = processResponse(response)
            if(result){
                logger.trace{"${name}: follow response from the request ${response.request.name}"}
                responseOut.send(response)
            }
        }
        else{
            throw NoRequestSenderException("No request found for this sender")
        }
    }

    /**
     * Processes a response
     * @param response the response to be processed
     * @return true if the response should be followed to the previous piece, false otherwise.
     */
    suspend fun processResponse(response: Response): Boolean


    override suspend fun run() {
        super<RequestReceiver>.run()
        super<RequestSender>.run()
    }

    override suspend fun stop() {
        super<RequestReceiver>.stop()
        super<RequestSender>.stop()
    }
}


abstract class AbstractMiddleware(override val name: String): Middleware{
    override val mutex: Mutex = Mutex()
    override var state: State = State()
    override var pendingRequests: PendingRequestMap = PendingRequestMap()

    override lateinit var requestIn: ReceiveChannel<AbstractRequest>
    override lateinit var responseOut: SendChannel<Response>

    override lateinit var requestOut: SendChannel<AbstractRequest>
    override lateinit var responseIn: ReceiveChannel<Response>

    override lateinit var scope: CoroutineScope

}
/**
 *
 */
abstract class SpiderMiddleware(name:String):
    AbstractMiddleware(name), ItemFollower {

    override lateinit var itemIn: ReceiveChannel<Item>
    override lateinit var itemOut: SendChannel<Item>

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        super<AbstractMiddleware>.run()
        super<ItemFollower>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping spider middleware"}
        super<AbstractMiddleware>.stop()
        super<ItemFollower>.stop()
    }
}

abstract class DownloaderMiddleware(name: String): AbstractMiddleware(name){

    override suspend fun stop() {
        super.stop()
    }
}
