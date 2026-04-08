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
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.dispatchers.AbstractCombinator
import org.sbm4j.meercat.nodes.dispatchers.BackDispatcher
import org.sbm4j.meercat.nodes.dispatchers.Barrier
import org.sbm4j.meercat.nodes.logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


class SpiderDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    val di: DI
): BackDispatcher, Barrier, AbstractCombinator(), Component{

    override var state: State = State()
    override val pendingAnswerable: MutableMap<UUID, SuperChannel> = ConcurrentHashMap()
    override val pendingBarrier: ConcurrentHashMap<String, MutableList<Pair<Send, Int>>> = ConcurrentHashMap()

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
        super<Barrier>.performSendBacks(
            Event::class,
            EventBack::class,
            null)
    }

    override suspend fun run() {
        performSendBacks(AbstractRequest::class, Response::class)
        super<BackDispatcher>.performSends(Internal::class, null, ::performInternal)
        super<AbstractCombinator>.run()
    }

    fun addBranch(channel: SuperChannel){
        this.channelsIns.add(channel)
    }
}