package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.KSuspendFunction2

typealias Callback = suspend (Response) -> Unit
typealias CallbackError = suspend (Throwable) -> Unit
typealias PendingRequestMap = ConcurrentHashMap<Int, Channel<Response>>

class NoRequestSenderException(message: String) : Exception(message)

class RequestException(message: String, val resp: Response, cause: Throwable? = null) :
    Exception(message, cause)



/**
 * An object from the kt scraping line that can send requests and receive responses.
 * The object is fully asynchronous, so the requests are sent in a coroutine while the
 * responses are received in another coroutine.
 * @property mutex a mutex that allows to safely executes callbacks and process response as they can modify the shared state
 * @property pendingRequests the request that wait for a response
 * @property requestOut the channel used to send the requests
 * @property responseIn the channel used to receive the responses
 * @author Sandrine Ben Mabrouk
 */
interface  RequestSender: Controllable {

    var pendingRequests: PendingRequestMap

    val requestOut: SendChannel<AbstractRequest>

    val responseIn: ReceiveChannel<Response>

    suspend fun peformSend(request: AbstractRequest, callback: Callback, callbackError: CallbackError? = null){
        val respChannel = Channel<Response>(Channel.RENDEZVOUS)
        pendingRequests[request.reqId] = respChannel
        logger.debug { "${name}: sends the request ${request.name} and waits for a response" }
        requestOut.send(request)
        val response = respChannel.receive()
        respChannel.close()
        logger.debug { "${name}: received the response for the request ${response.request.name} and call callback" }
        mutex.withLock {
            try {
                when (response.status) {
                    Status.OK -> callback(response)
                    else -> {
                        if (callbackError != null){
                            val ex = RequestException("Error when fetching the request ${request.name}", response)
                            callbackError(ex)
                        }
                        else callback(response)
                    }
                }
            }
            catch(ex: Exception){
                val message = "Error while executing callback from the request ${request.name}"
                logger.error(ex){ message }
                if(callbackError != null){
                    callbackError(RequestException(message, response, ex))
                }
            }
        }
    }

    /**
     * Sends a new created request in a new coroutine and executes the callback when receiving the response
     * @param request the request to be sent
     * @param callback the callback to be executed
     */
    suspend fun send(request: AbstractRequest, callback: Callback, callbackError: CallbackError? = null) {
        scope.launch(CoroutineName("${name}-${request.name}")){
            this@RequestSender.peformSend(request, callback, callbackError)
        }
    }

    /**
     * Receive all the responses.
     * If the response corresponds to a new created requests sent by this object, the response is sent
     * to the corresponding coroutine in order to execute the callback, otherwise the response is processed.
     */
    suspend fun receiveResponses() {
        scope.launch(CoroutineName("${name}-performResponses")) {
            logger.debug { "${name}: Waits for responses to process" }
            for (resp in responseIn) {

                val req = resp.request
                val reqId = req.reqId
                if (req.sender == this@RequestSender && reqId in pendingRequests.keys) {
                    logger.debug{ "${name}: Dispatch response to the request coroutine" }
                    val respChannel = pendingRequests[reqId]
                    pendingRequests.remove(reqId)
                    respChannel?.send(resp)
                } else {
                    logger.debug{"${name}: Process response"}
                    mutex.withLock {
                        try{
                            performResponse(resp)
                        }
                        catch(ex: Exception){
                            logger.error(ex){ "${name}: Error while processing response"}
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes a response
     * @param response the response to be processed
     */
    suspend fun performResponse(response: Response)

    override suspend fun start() {
        this.receiveResponses()
    }

    override suspend fun stop() {
        this.requestOut.close()
    }
}
