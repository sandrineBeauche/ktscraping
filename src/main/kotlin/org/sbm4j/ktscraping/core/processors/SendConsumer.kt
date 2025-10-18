package org.sbm4j.ktscraping.core.processors

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.Status
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

interface SendConsumer: Controllable {

    var inChannel: SuperChannel

    suspend fun <T: Send> performSends(
        sendClazz: KClass<T>,
        flow: Flow<T>,
        func: suspend (T) -> Any?
    ){
        scope.launch(CoroutineName("${name}-perform${sendClazz.simpleName}")) {
            logger.debug { "${name}: Waits for ${sendClazz.simpleName} to process" }
            flow.collect{ send ->
                this.launch() {
                    try {
                        logger.trace { "${name}: received ${send.loggingLabel} ${send.channelableId}: $send" }
                        val result: Any? = func(send)

                        if ((result is Boolean && result) || result != null) {
                            sendPostProcess(send, result)
                        }
                    }
                    catch(ex: Exception){
                        logger.error{ "${this@SendConsumer.name}: error when processing ${sendClazz.simpleName} ${send.channelableId} - ${ex.message}" }
                        val infos = generateErrorInfos(ex)
                        val back = send.buildErrorBack(infos)
                        inChannel.send(back)
                    }
                }
                logger.trace { "${name}: ready to receive another ${sendClazz.simpleName}" }
            }
            logger.debug{"${name}: Finished to receive ${sendClazz.simpleName}"}
        }
    }

    suspend fun sendPostProcess(send: Send, result: Any)
}


interface SendSource: Controllable {

    var outChannel: SuperChannel

    private suspend fun <S: Send> peformSendSync(
        send: S,
        callback: (Back<S>) -> Unit,
        callbackError: CallbackError? = null
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
            this@SendSource.peformSendSync<S>(request, continuation::resume,
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
            this@SendSource.peformSendSync(request, callback, callbackError)
        }
    }
}

interface SendForwarder : Controllable, SendSource, SendConsumer{

    override suspend fun sendPostProcess(send: Send, result: Any) {
        if(result is Back<*>){
            logger.trace { "${name}: returns a ${result.loggingLabel} for the ${send.loggingLabel} ${send.name}" }
            inChannel.send(result)
        }
        else {
            logger.trace { "${name}: forward ${send.loggingLabel} ${send.name}" }
            outChannel.send(send)
        }
    }

}