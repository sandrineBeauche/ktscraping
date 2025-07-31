package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias Callback = suspend (Response<*>) -> Unit
typealias CallbackError = suspend (Throwable) -> Unit
typealias PendingRequestMap = ConcurrentHashMap<Int, Channel<Response<*>>>

class NoRequestSenderException(message: String) : Exception(message)

class RequestException(message: String, val resp: Response<*>, cause: Throwable? = null) :
    Exception(message, cause)



/**
 * A component from the kt scraping line that can send requests and receive responses.
 * The object is fully asynchronous, so the requests are sent in a coroutine while the
 * responses are received in another coroutine.
 * @property mutex a mutex that allows to safely executes callbacks and process response as they can modify the shared state
 * @property pendingRequests the request that wait for a response
 * @property requestOut the channel used to send the requests
 * @property responseIn the channel used to receive the responses
 * @author Sandrine Ben Mabrouk
 */
interface  RequestSender: Controllable, EventConsumer {

    var pendingRequests: PendingRequestMap

    val requestOut: SendChannel<AbstractRequest>

    val responseIn: ReceiveChannel<Response<*>>

    val pendingMinorError: ConcurrentHashMap<Int, MutableList<ErrorInfo>>

    private suspend fun peformSend(request: AbstractRequest, callback: Callback, callbackError: CallbackError? = null){
        val respChannel = Channel<Response<*>>(Channel.RENDEZVOUS)
        pendingRequests[request.reqId] = respChannel
        logger.trace { "${name}: sends the request ${request.name} and waits for a response" }
        requestOut.send(request)
        val response = respChannel.receive()
        respChannel.close()
        logger.trace { "${name}: received the response for the request ${response.request.name} and call callback" }

        try {
            when (response.status) {
                Status.OK -> callback(response)
                else -> {
                    if (callbackError != null) {
                        val ex = RequestException("Error when fetching the request ${request.name}", response)
                        callbackError(ex)
                    } else callback(response)
                }
            }
        } catch (ex: Exception) {
            val message = "Error while executing callback from the request ${request.name}"
            logger.error(ex) { message }
            if (callbackError != null) {
                callbackError(RequestException(message, response, ex))
            }
        }

    }

    /**
     * sends synchronously a request and returns the response. This exchange with the request and the response
     * is done in a dedicated scope, that is a subscope of the given coroutine scope.
     * @param request the request to be sent
     * @param subScope the parent scope of the scope where the request is sent and the response is received
     * @throws RequestException if the response status is not OK
     */
    suspend fun sendSync(
        request: AbstractRequest,
        subScope: CoroutineScope = scope
    ) = suspendCoroutine<Response<*>> { continuation ->
        subScope.launch(CoroutineName("${name}-${request.name}")) {
            this@RequestSender.peformSend(request, continuation::resume, continuation::resumeWithException)
        }
    }

    /**
     * Sends a request in a new coroutine and executes the callback when receiving the response
     * @param request the request to be sent
     * @param callback the callback to be executed
     */
    suspend fun send(
        request: AbstractRequest,
        callback: Callback,
        callbackError: CallbackError? = null,
        subScope: CoroutineScope = scope
    ) {
        subScope.launch(CoroutineName("${name}-${request.name}")){
            this@RequestSender.peformSend(request, callback, callbackError)
        }
    }

    /**
     * Receive all the responses.
     * If the response corresponds to a new created requests sent by this component, the response is sent
     * to the corresponding coroutine in order to execute the callback, otherwise the response is processed.
     */
    suspend fun receiveResponses() {
        scope.launch(CoroutineName("${name}-performResponses")) {
            logger.debug { "${name}: Waits for responses to process" }
            for (resp in responseIn) {
                logger.trace { "${name}: received a response for the request ${resp.request.name}" }
                scope.launch(CoroutineName("${name}-performResponse-${resp.request.name}")) {
                    performResponse(resp)
                }
                logger.trace{"$name: ready to receive another response"}
            }
            logger.debug { "${name}: Finished receiving responses" }
        }
    }


    suspend fun performResponse(response: Response<*>){
        val req = response.request
        val reqId = req.reqId
        if (req.sender == this@RequestSender && reqId in pendingRequests.keys) {
            dispatchResponse(response)
        } else {
            logger.trace{"${name}: Process response for the request ${response.request.name}"}

                try{
                    val errors = pendingMinorError.remove(response.request.reqId)
                    if(errors != null && errors.isNotEmpty()){
                        response.status = Status.ERROR
                        response.errorInfos.addAll(errors)
                    }
                    when(response){
                        is EventResponse -> resumeEvent(response)
                        is DownloadingResponse -> performDownloadingResponse(response, response.request)
                    }
                }
                catch(ex: Exception){
                    logger.error(ex){ "${name}: Error while processing response - ${ex.message}"}
                }

        }
    }


    suspend fun dispatchResponse(response: Response<*>){
        logger.trace{ "${name}: Dispatch response for the request ${response.request.name} to the request coroutine" }
        val respChannel = pendingRequests.remove(response.request.reqId)
        respChannel?.send(response)
    }


    /**
     * Processes a downloading response
     * @param response the response to be processed
     */
    suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest)


    override suspend fun run() {
        this.receiveResponses()
    }

    override suspend fun stop() {
        this.requestOut.close()
    }
}
