package org.sbm4j.ktscraping.core.channels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Send
import kotlin.reflect.KClass
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.repackaged.net.bytebuddy.implementation.bind.annotation.Super
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.example.main
import java.util.concurrent.CountDownLatch
import kotlin.collections.reduce


class SuperChannel() {

    companion object{
        suspend fun build(): SuperChannel{
            val result = SuperChannel()
            result.init()
            return result
        }
    }

    val channel: Channel<Channelable> = Channel(Channel.UNLIMITED)


    lateinit var mainFlow: SharedFlow<Channelable>

    lateinit var scope: CoroutineScope


    suspend fun init() = coroutineScope{
        logger.debug{ "initialize superChannel"}

        this@SuperChannel.scope = CoroutineScope(Dispatchers.Default)

        val deferred = async {
            val flow = channel.consumeAsFlow()
            flow.shareIn(this@SuperChannel.scope, SharingStarted.WhileSubscribed())
        }

        mainFlow = deferred.await()
        logger.debug{"superchannel initialized with success"}
    }

    suspend inline fun <reified T: Back<*>> sendSync(
        data: Send,
    ): T{
        channel.send(data)
        val result = mainFlow.filterIsInstance<T>()
            .first { it.send.channelableId == data.channelableId }
        return result
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

    suspend fun receiveAllSend(): Send{
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

    val result = backs.reduce { b1, b2 -> b1 + b2 }
    return@coroutineScope result
}
