package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Send
import kotlin.reflect.KClass

class SuperChannel() {

    val channel: Channel<Channelable> = Channel(Channel.UNLIMITED)


    lateinit var mainFlow: SharedFlow<Channelable>

    lateinit var scope: CoroutineScope


    fun init(scope: CoroutineScope){
        mainFlow = channel.consumeAsFlow().shareIn(scope, SharingStarted.WhileSubscribed())
    }

    suspend inline fun <reified T: Back<*>> sendSync(
        data: Send,
    ): T{
        channel.send(data)
        val result = mainFlow.filterIsInstance<T>()
            .first { it.send.channelableId == data.channelableId }
        return result
    }

    inline fun <reified T: Send> getFlow(): Flow<T> {
        return mainFlow.filterIsInstance<T>()
    }

    fun <T: Send> getFlow(clazz: KClass<T>): Flow<T> {
        return mainFlow.filterIsInstance(clazz)
    }

    inline fun <reified T: Back<*>> getFlow(component: Controllable? = null): Flow<T> {
        val f = mainFlow.filterIsInstance<T>()
        return if(component == null){
            f
        } else{
            f.filter { it.send.sender != component }
        }
    }

    fun <T: Back<*>> getFlow(clazz: KClass<T>, component: Controllable? = null): Flow<T>{
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

    fun close(){
        channel.close()
        if(scope.isActive) {
            scope.cancel()
        }
    }
}