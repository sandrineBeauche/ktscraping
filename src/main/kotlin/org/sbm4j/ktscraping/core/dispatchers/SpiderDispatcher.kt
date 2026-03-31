package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Send
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.meercat.nodes.dispatchers.BackDispatcher
import org.sbm4j.meercat.nodes.dispatchers.Barrier
import org.sbm4j.meercat.nodes.logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


class SpiderDispatcher(
    override val name: String = "SpiderResponseDispatcher",
    val di: DI
): BackDispatcher, Barrier, AbstractComponent(){

    override val channelsIns: MutableList<SuperChannel> = mutableListOf()

    override lateinit var channelOut: SuperChannel


    override val pendingAnswerable: MutableMap<UUID, SuperChannel> = ConcurrentHashMap()


    override val pendingBarrier: ConcurrentHashMap<String, MutableList<Pair<Send, Int>>> = ConcurrentHashMap()
    override suspend fun <T : Send, B : Back<T>> performSendBacks(
        clazz: KClass<T>,
        backClazz: KClass<B>,
        predicate: ((T) -> Boolean)?
    ) {
        TODO("Not yet implemented")
    }


    /*
    override suspend fun performSendBacks(predicate: (suspend (Send) -> Boolean)?) {
        //super<Barrier>.performSendBacks { it is Event }
        //super<BackDispatcher>.performSendBacks { it is AbstractRequest }
    }

     */

    override suspend fun run() {
        //performSendBacks()
    }

    fun addBranch(channel: SuperChannel){
        this.channelsIns.add(channel)
    }
}