package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.core.components.State
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.internal.Internal
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.dispatchers.AbstractCombinator
import org.sbm4j.meercat.nodes.dispatchers.BackDispatcher
import org.sbm4j.meercat.nodes.dispatchers.Barrier
import org.sbm4j.meercat.nodes.logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Central combinator node collecting messages from multiple Spider branches toward the Engine.
 *
 * [SpiderDispatcher] is the symmetric counterpart of [AbstractEngine]: where the Engine
 * dispatches messages from a single Spider channel to the Downloader and Pipeline branches,
 * the [SpiderDispatcher] collects messages from multiple Spider branches and funnels them
 * into a single output channel toward the Engine.
 *
 * It combines two Meercat combinator strategies depending on the message type:
 * - [BackDispatcher] for [AbstractRequest] and [Item]: memorizes the source Spider branch
 *   in [pendingAnswerable] so that [Response] and [ItemAck] backs are routed back to the
 *   correct Spider.
 * - [Barrier] for [Event]: synchronizes all Spider branches on the event name (via
 *   [Event.getKeyBarrier]) before forwarding the aggregated [EventBack] to all branches.
 * - [Internal] messages are forwarded fire-and-forget to [channelOut] (the Engine)
 *   with no back expected, consistent with their unidirectional nature.
 *
 * Spider branches are registered dynamically at topology startup via [addBranch].
 *
 * @see BackDispatcher
 * @see Barrier
 * @see AbstractCombinator
 * @see AbstractEngine
 */
class SpiderDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    val di: DI
): BackDispatcher, Barrier, AbstractCombinator(), Component{

    override var state: State = State()

    /** Pending back dispatch map, keyed by message [UUID], used by [BackDispatcher]. */
    override val pendingAnswerable: MutableMap<UUID, SuperChannel> = ConcurrentHashMap()

    /** Pending barrier synchronization map, keyed by barrier key, used by [Barrier]. */
    override val pendingBarrier: ConcurrentHashMap<String, MutableList<Pair<Send, Int>>> = ConcurrentHashMap()

    /**
     * Forwards an [Internal] message to [channelOut] (the Engine) without expecting a back.
     *
     * @param send The internal message to forward.
     * @param sender The source Spider channel.
     * @param index The index of the source branch.
     */
    suspend fun <T: Send> performInternal(send: T, sender: SuperChannel, index: Int){
        logger.trace { "Received send ${send.name} from input #$index and forwards it" }
        channelOut.send(send)
    }

    override suspend fun <T : Send, B : Back<T>> performSendBacks(
        clazz: KClass<T>,
        backClazz: KClass<B>,
        predicate: ((T) -> Boolean)?
    ) {
        super<BackDispatcher>.performSendBacks(
            clazz,
            backClazz,
            predicate)
    }

    override suspend fun run() {
        // Route Request/Response backs to the originating Spider via BackDispatcher
        performSendBacks(AbstractRequest::class, Response::class)

        // Route Item/ItemAck backs to the originating Spider via BackDispatcher
        performSendBacks(Item::class, ItemAck::class)

        // Synchronize all Spider branches on Event name via Barrier before forwarding
        super<Barrier>.performSendBacks(
            Event::class,
            EventBack::class,
            null)

        // Forward Internal messages fire-and-forget to the Engine
        super<BackDispatcher>.performSends(
            Internal::class, null,
            ::performInternal)
        super<AbstractCombinator>.run()
    }

    /**
     * Registers a new Spider branch as an input channel.
     *
     * Called during topology construction to connect a Spider's output channel
     * to this dispatcher.
     *
     * @param channel The [SuperChannel] of the Spider branch to add.
     */
    fun addBranch(channel: SuperChannel){
        this.channelsIns.add(channel)
    }
}