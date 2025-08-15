package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import kotlin.reflect.KClass

interface SendConsumer<T: Send>: Controllable{
    var inChannel: SuperChannel


    @OptIn(DelicateCoroutinesApi::class)
    suspend fun performSends(sendClazz: KClass<T>){
        scope.launch(CoroutineName("${name}-perform${sendClazz.simpleName}")) {
            logger.debug { "${name}: Waits for ${sendClazz.simpleName} to process" }
            inChannel.getFlow(sendClazz).collect{ send ->
                this.launch() {
                    performSend(send, sendClazz)
                }
                logger.trace { "${name}: ready to receive another ${sendClazz.simpleName}" }
            }
            logger.debug{"${name}: Finished to receive ${sendClazz.simpleName}"}
        }
    }

    suspend fun performSend(send: T, sendClazz: KClass<T>){
        try {
            logger.trace { "${name}: received request ${send.channelableId}: ${send}" }
            val result: Any? = processSend(send)

            if ((result is Boolean && result) || result != null) {
                sendPostProcess(send, result)
            }
        }
        catch(ex: Exception){
            logger.error{ "${this@SendConsumer.name}: error when processing ${sendClazz.simpleName} ${send.channelableId} - ${ex.message}" }
            val infos = ErrorInfo(ex, this@SendConsumer, ErrorLevel.MAJOR)
            val back = send.buildErrorBack(infos)
            inChannel.send(back)
        }
    }

    suspend fun processSend(send: T): Any?

    suspend fun sendPostProcess(send: T, result: Any)

}