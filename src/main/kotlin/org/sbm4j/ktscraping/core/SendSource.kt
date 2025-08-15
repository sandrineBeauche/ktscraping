package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

interface SendSource<T: Send, B: Back<T>>: Controllable {

    var outChannel: SuperChannel

    val sendClazz: KClass<*>


    val pendingMinorError: ConcurrentHashMap<UUID, MutableList<ErrorInfo>>

    private suspend inline fun <S: Send> peformSend(
        send: S,
        callback: (Back<S>) -> Unit,
        noinline callbackError: CallbackError? = null
    ){
        val back = outChannel.sendSync<Back<S>>(send)

        logger.trace { "${name}: received the ${back.loggingLabel} for the ${send.loggingLabel} ${send.name} and call callback" }

        try {
            when (back.status) {
                Status.OK -> callback(back)
                else -> {
                    if (callbackError != null) {
                        val ex = SendException("Error when fetching the ${send.loggingLabel} ${send.sender}", back)
                        callbackError(ex)
                    } else callback(back)
                }
            }
        } catch (ex: Exception) {
            val message = "Error while executing callback from the ${send.loggingLabel} ${send.name}"
            logger.error(ex) { message }
            if (callbackError != null) {
                callbackError(SendException(message, back, ex))
            }
        }
    }

    /**
     * sends synchronously a request and returns the response. This exchange with the request and the response
     * is done in a dedicated scope, that is a subscope of the given coroutine scope.
     * @param request the request to be sent
     * @param subScope the parent scope of the scope where the request is sent and the response is received
     * @throws SendException if the response status is not OK
     */
    suspend fun <S: Send> sendSync(
        request: S,
        subScope: CoroutineScope = scope
    ) = suspendCoroutine { continuation ->
        subScope.launch(CoroutineName("${name}-${request.name}")) {
            this@SendSource.peformSend<S>(request, continuation::resume,
                continuation::resumeWithException)
        }
    }

    /**
     * Sends a request in a new coroutine and executes the callback when receiving the response
     * @param request the request to be sent
     * @param callback the callback to be executed
     */
    suspend fun <S: Send> send(
        request: S,
        callback: (Back<S>) -> Unit,
        callbackError: CallbackError? = null,
        subScope: CoroutineScope = scope
    ) {
        subScope.launch(CoroutineName("${name}-${request.name}")){
            this@SendSource.peformSend(request, callback, callbackError)
        }
    }

    /**
     * Receive all the responses.
     * If the response corresponds to a new created requests sent by this component, the response is sent
     * to the corresponding coroutine in order to execute the callback, otherwise the response is processed.
     */
    suspend fun receiveBacks(backClazz: KClass<B>) {
        scope.launch(CoroutineName("${name}-perform${backClazz.simpleName}")) {
            logger.debug { "${name}: Waits for ${backClazz.simpleName} to process" }
            outChannel.getFlow(backClazz, this@SendSource).collect { back ->
                logger.trace { "${name}: received a ${backClazz.simpleName} for the ${sendClazz.simpleName} ${back.send.name}" }
                scope.launch(CoroutineName("${name}-perform${backClazz.simpleName}-${back.send.name}")) {
                    val errors = pendingMinorError.remove(back.send.channelableId)
                    if (errors != null && errors.isNotEmpty()) {
                        back.status = Status.ERROR
                        back.errorInfos.addAll(errors)
                    }

                    performBack(back)
                }
                logger.trace{"$name: ready to receive another ${backClazz.simpleName}"}
            }
            logger.debug { "${name}: Finished receiving ${backClazz.simpleName}" }
        }
    }


    suspend fun performBack(back: B)


}