package org.sbm4j.meercat.components

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Status
import org.sbm4j.meercat.channels.SuperChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


interface BackForwarder: Controllable {

    var inChannel: SuperChannel

    var outChannel: SuperChannel

    val pendingMinorError: ConcurrentHashMap<UUID, MutableList<ErrorInfo>>

    /**
     * Receive all the responses.
     * If the response corresponds to a new created requests sent by this component, the response is sent
     * to the corresponding coroutine in order to execute the callback, otherwise the response is processed.
     */
    suspend fun <B : Back<*>> receiveBacks(
        backClazz: KClass<B>,
        flow: Flow<B>,
        func: suspend (B) -> Unit
    ) {
        scope.launch(CoroutineName("${name}-perform${backClazz.simpleName}")) {
            logger.debug { "${name}: Waits for ${backClazz.simpleName} to process" }
            flow.collect { back ->
                logger.trace { "${name}: received a ${backClazz.simpleName} for the ${back.send::class.simpleName} ${back.send.name}" }
                scope.launch(CoroutineName("${name}-perform${backClazz.simpleName}-${back.send.name}")) {
                    val errors = pendingMinorError.remove(back.send.channelableId)
                    if (errors != null && errors.isNotEmpty()) {
                        back.status = Status.ERROR
                        back.errorInfos.addAll(errors)
                    }

                    logger.trace { "${name}: Process ${back.loggingLabel} for the ${back.send.loggingLabel} ${back.send.name}" }
                    try {
                        func(back)
                    } catch (ex: Exception) {
                        logger.error(ex) { "${name}: Error while processing ${back.loggingLabel} - ${ex.message}" }
                        val infos = generateErrorInfos(ex)
                        back.status = Status.ERROR
                        back.errorInfos.add(infos)
                    }
                    finally {
                        this@BackForwarder.inChannel.send(back)
                    }
                }
                logger.trace { "$name: ready to receive another ${backClazz.simpleName}" }
            }
            logger.debug { "${name}: Finished receiving ${backClazz.simpleName}" }
        }
    }
}