package org.sbm4j.meercat.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.sbm4j.ktscraping.core.childScope
import org.sbm4j.meercat.components.Controllable
import org.sbm4j.meercat.components.logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass


class SuperChannel(val name: String = "superChannel") {

    companion object{
        var lastId: AtomicInteger = AtomicInteger()
        suspend fun build(parentScope: CoroutineScope, init: Boolean = true): SuperChannel{
            val result = SuperChannel("superChannel#${lastId.getAndIncrement()}")
            if(init) {
                result.init(parentScope)
            }
            return result
        }
    }

    val channel: Channel<Channelable> = Channel(Channel.UNLIMITED)


    lateinit var mainFlow: SharedFlow<Channelable>

    lateinit var scope: CoroutineScope


    fun init(parentScope: CoroutineScope){
        logger.debug{ "initialize ${name}"}
        scope = childScope(parentScope, "${name}-root")

        val flow = channel.consumeAsFlow()
        mainFlow = flow.shareIn(scope, SharingStarted.WhileSubscribed(0), replay = 0)

        logger.debug{"${name} initialized with success"}
    }

    suspend inline fun <reified T: Back<*>>
            sendSync(
        data: Send,
    ): T{
        val job = Job(scope.coroutineContext[Job])
        val sendScope = CoroutineScope(scope.coroutineContext + job + CoroutineName("${name}-sendSync"))

        try {
            return withContext(sendScope.coroutineContext) {
                val flow = mainFlow.filterIsInstance<T>().filter { it.send.channelableId == data.channelableId }
                logger.trace { "${name} -> send message : ${data}" }
                channel.send(data)
                logger.trace { "${name} -> sent message : ${data} and wait for a response" }
                val result = flow.first()
                logger.trace { "${name} -> received response: ${result}" }
                result
            }
        }
        finally{
            job.cancel()
        }
    }

    fun  getSendFlow(): Flow<Send> {
        return mainFlow.filterIsInstance(Send::class)
    }

    fun <T1: Send> getSendFlow(clazz: KClass<T1>): Flow<T1> {
        return mainFlow.filterIsInstance(clazz)
    }

    fun getBackFlow(component: Controllable? = null): Flow<Back<*>> {
        val f = mainFlow.filterIsInstance(Back::class)
        return if(component == null){
            f
        } else{
            f.filter { it.send.sender != component }
        }
    }

    fun <B1: Back<*>> getBackFlow(clazz: KClass<B1>, component: Controllable? = null): Flow<B1>{
        val f = mainFlow.filterIsInstance(clazz)
        return if(component == null){
            f
        } else{
            f.filter { it.send.sender != component }
        }
    }


    suspend fun send(data: Channelable){
        channel.send(data)
    }

    suspend fun receiveAllSend(): Send {
        return mainFlow.filterIsInstance(Send::class).first()
    }

    suspend inline fun <reified T1: Send> receiveSend(): T1{
        return mainFlow.filterIsInstance<T1>().first()
    }

    suspend inline fun <reified B1: Back<*>> receiveBack(): B1{
        return mainFlow.filterIsInstance<B1>().first()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun close(){
        if(!channel.isClosedForReceive || !channel.isClosedForSend) {
            channel.close()
        }
        if(scope.isActive) {
            scope.cancel()
        }
    }
}

suspend fun sendSyncAll(senders: List<SuperChannel>, send: Send): Back<*> = coroutineScope {
    val deferredBacks = senders.map{ channel ->
        async{
            val cl = send.clone()
            channel.sendSync<Back<*>>(cl)
        }
    }

    val backs = deferredBacks.awaitAll()
    logger.debug{ "superchannels: received all the backs, reducing ${backs}"}

    val result = backs.reduce { b1, b2 -> b1 + b2 }
    result.send.channelableId = send.channelableId

    logger.debug{"superchannels: reduce result is ${result}"}
    return@coroutineScope result
}


suspend fun sendSyncAll(sends: Map<SuperChannel, Send>): List<Back<*>> = coroutineScope {
    val deferredBacks = sends.map{ (channel, send) ->
        async{
            channel.sendSync<Back<*>>(send)
        }
    }
    val backs = deferredBacks.awaitAll()

    return@coroutineScope backs
}
