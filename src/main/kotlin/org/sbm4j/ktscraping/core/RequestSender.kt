package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

typealias CallbackError = suspend (Throwable) -> Unit
typealias PendingRequestMap = ConcurrentHashMap<UUID, Channel<Response<*>>>

class NoRequestSenderException(message: String) : Exception(message)

class SendException(message: String, val resp: Back<*>, cause: Throwable? = null) :
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
interface  RequestSender: EventConsumer {

    val name: String


    var outChannel: SuperChannel

    suspend fun performBack(back: Response<*>) {
        logger.trace { "${name}: Process ${back.loggingLabel} for the ${back.send.loggingLabel} ${back.send.name}" }

        try {
            when (back) {
                is EventResponse -> resumeEvent(back)
                is DownloadingResponse -> performDownloadingResponse(back, back.send)
            }
        } catch (ex: Exception) {
            logger.error(ex) { "${name}: Error while processing ${back.loggingLabel} - ${ex.message}" }
        }
    }


    suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        var result = true
        try {
            result = processDownloadingResponse(response, response.send)
        }
        catch(ex: Exception){
            val infos = generateErrorInfos(ex)
            response.status = Status.ERROR
            response.errorInfos.add(infos)
        }
        if(result){
            logger.trace{"${name}: forward ${response.loggingLabel} from the ${request.loggingLabel} ${response.send.name}"}
            outChannel.send(response)
        }
    }

    /**
     * Processes a response
     * @param response the response to be processed
     * @return true if the response should be followed to the previous piece, false otherwise.
     */
    suspend fun processDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean

}
