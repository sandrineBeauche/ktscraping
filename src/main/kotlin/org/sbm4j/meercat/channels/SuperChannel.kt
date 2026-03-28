package org.sbm4j.meercat.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.sbm4j.meercat.childScope
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Channelable
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.exemples.main
import org.sbm4j.meercat.nodes.Node
import org.sbm4j.meercat.nodes.logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * A bidirectional, typed communication channel between nodes in the Meercat topology.
 *
 * A [SuperChannel] acts as a shared message bus over which both [Send] and [Back] messages
 * can transit. It is built on top of a Kotlin [Channel] and exposes a [SharedFlow] that
 * allows multiple nodes to subscribe and react to incoming messages concurrently.
 *
 * [SuperChannel] instances should be created using the [build] factory function, which
 * handles initialization within a given coroutine scope.
 *
 * @property name a human-readable name for this channel, used in logging
 */
class SuperChannel(val name: String = "superChannel") {

    companion object{
        /**
         * A shared atomic counter used to generate unique names for [SuperChannel] instances.
         */
        var lastId: AtomicInteger = AtomicInteger()

        /**
         * Creates and optionally initializes a new [SuperChannel] instance.
         *
         * @param parentScope the coroutine scope to use as parent for this channel's internal scope
         * @param init whether to immediately initialize the channel, defaults to `true`
         * @return a new [SuperChannel] instance
         */
        suspend fun build(
            parentScope: CoroutineScope,
            init: Boolean = true,
            name: String = "superChannel#${lastId.getAndIncrement()}"
        ): SuperChannel{
            val result = SuperChannel(name)
            if(init) {
                result.init(parentScope)
            }
            return result
        }
    }

    /**
     * The underlying Kotlin [Channel] with unlimited capacity over which [Channelable] messages transit.
     */
    val channel: Channel<Channelable> = Channel(Channel.UNLIMITED)

    /**
     * The [SharedFlow] that exposes messages from [channel] to all subscribers.
     * Initialized by [init].
     */
    lateinit var mainFlow: SharedFlow<Channelable>

    /**
     * The coroutine scope owned by this channel, used to host the [mainFlow] sharing coroutine.
     * Initialized by [init].
     */
    lateinit var scope: CoroutineScope

    /**
     * Initializes this [SuperChannel] by creating a child coroutine scope and setting up
     * the [mainFlow] as a shared flow consuming from [channel].
     *
     * @param parentScope the coroutine scope to use as parent for this channel's internal scope
     */
    fun init(parentScope: CoroutineScope){
        logger.debug{ "initialize ${name}"}
        scope = childScope(parentScope, "${name}-root")

        val flow = channel.consumeAsFlow()
        mainFlow = flow.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

        logger.debug{"${name} initialized with success"}
    }

    /**
     * Sends a [Send] message through the channel and suspends until the matching [Back] response
     * of type [T] is received.
     *
     * The match between the [Send] and its [Back] is done using [Channelable.channelableId],
     * ensuring that the correct response is returned even in a concurrent context.
     * The internal coroutine scope used for this operation is cancelled once the response
     * is received or an error occurs.
     *
     * @param T the expected type of the [Back] response
     * @param data the [Send] message to dispatch
     * @return the [Back] response of type [T] matching the sent message
     */
    suspend inline fun <reified T: Back<*>> sendSync(
        data: Send,
    ): T{
        val job = Job(scope.coroutineContext[Job])
        val sendScope = CoroutineScope(scope.coroutineContext + job + CoroutineName("${name}-sendSync"))

        try {
            return withContext(sendScope.coroutineContext) {
                val flow =
                    mainFlow.filterIsInstance<T>()
                        .filter { it.send.channelableId == data.channelableId }
                        .onStart {
                            logger.trace { "${name} -> send message : ${data}" }
                            channel.send(data)
                            logger.trace { "${name} -> sent message : ${data} and wait for a response" }
                        }
                val result = flow.first()
                logger.trace { "${name} -> received response: ${result}" }
                result
            }
        }
        finally{
            job.cancel()
        }
    }

    /**
     * Returns a [Flow] of all [Send] messages transiting through this channel.
     *
     * @return a [Flow] emitting all [Send] messages
     */
    fun  getSendFlow(): Flow<Send> {
        return mainFlow.filterIsInstance(Send::class)
    }

    /**
     * Returns a [Flow] of [Send] messages of a specific type transiting through this channel.
     *
     * @param T1 the specific [Send] type to filter for
     * @param clazz the [KClass] of the expected [Send] type
     * @return a [Flow] emitting only [Send] messages of type [T1]
     */
    fun <T1: Send> getSendFlow(clazz: KClass<T1>): Flow<T1> {
        return mainFlow.filterIsInstance(clazz)
    }

    /**
     * Returns a [Flow] of [Back] messages transiting through this channel.
     *
     * If a [Component] is provided, [Back] messages whose original [Send] was emitted
     * by that component are filtered out, allowing a node to ignore its own messages.
     *
     * @param component an optional [Component] to exclude from the flow, defaults to `null`
     * @return a [Flow] emitting [Back] messages, optionally filtered by sender
     */
    fun getBackFlow(component: Component? = null): Flow<Back<*>> {
        val f = mainFlow.filterIsInstance(Back::class)
        return if(component == null){
            f
        } else{
            f.filter { it.send.sender != component }
        }
    }

    /**
     * Returns a [Flow] of [Back] messages of a specific type transiting through this channel.
     *
     * If a [Node] is provided, [Back] messages whose original [Send] was emitted
     * by that node are filtered out.
     *
     * @param B1 the specific [Back] type to filter for
     * @param clazz the [KClass] of the expected [Back] type
     * @param component an optional [Node] to exclude from the flow, defaults to `null`
     * @return a [Flow] emitting [Back] messages of type [B1], optionally filtered by sender
     */
    fun <B1: Back<*>> getBackFlow(clazz: KClass<B1>, component: Node? = null): Flow<B1>{
        val f = mainFlow.filterIsInstance(clazz)
        return if(component == null){
            f
        } else{
            f.filter { it.send.sender != component }
        }
    }

    /**
     * Sends any [Channelable] message through this channel.
     *
     * @param data the [Channelable] message to dispatch
     */
    suspend fun send(data: Channelable){
        channel.send(data)
    }

    /**
     * Suspends until the next [Send] message of any type is received through this channel.
     *
     * @return the first [Send] message received
     */
    suspend fun receiveAllSend(): Send {
        return mainFlow.filterIsInstance(Send::class).first()
    }

    /**
     * Suspends until the next [Send] message of type [T1] is received through this channel.
     *
     * @param T1 the expected [Send] type
     * @return the first [Send] message of type [T1] received
     */
    suspend inline fun <reified T1: Send> receiveSend(): T1{
        return mainFlow.filterIsInstance<T1>().first()
    }

    /**
     * Suspends until the next [Back] message of type [B1] is received through this channel.
     *
     * @param B1 the expected [Back] type
     * @return the first [Back] message of type [B1] received
     */
    suspend inline fun <reified B1: Back<*>> receiveBack(): B1{
        return mainFlow.filterIsInstance<B1>().first()
    }

    /**
     * Closes this channel and cancels its internal coroutine scope.
     *
     * Safe to call even if the channel or scope is already closed or cancelled.
     */
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

/**
 * Sends a cloned copy of [send] through each [SuperChannel] in [senders] concurrently,
 * then aggregates all the resulting [Back] responses into a single one.
 *
 * Each channel receives an independent clone of [send] to avoid shared state between branches.
 * The individual [Back] responses are merged using the [Back.plus] operator, which aggregates
 * statuses and [ErrorInfo] entries. The [Channelable.channelableId] of the original [send]
 * is restored on the aggregated result to preserve message traceability.
 *
 * This function is typically used when a [Send] message must be dispatched to multiple branches
 * in parallel and their responses need to be combined into a single [Back].
 *
 * @param senders the list of [SuperChannel] instances to send the message through
 * @param send the [Send] message to dispatch to all channels
 * @return the aggregated [Back] response combining all individual responses
 */
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

/**
 * Sends each [Send] message through its associated [SuperChannel] concurrently,
 * and returns the list of resulting [Back] responses in the same order as the input map.
 *
 * Unlike [sendSyncAll(List, Send)], this overload allows each channel to receive
 * a distinct [Send] message, giving finer control over what is dispatched to each branch.
 * The responses are not aggregated — they are returned individually as a list.
 *
 * @param sends a map associating each [SuperChannel] with the [Send] message to dispatch through it
 * @return the list of [Back] responses, one per channel, in iteration order of [sends]
 */
suspend fun sendSyncAll(sends: Map<SuperChannel, Send>): List<Back<*>> = coroutineScope {
    val deferredBacks = sends.map{ (channel, send) ->
        async{
            channel.sendSync<Back<*>>(send)
        }
    }
    val backs = deferredBacks.awaitAll()

    return@coroutineScope backs
}
