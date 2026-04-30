package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DIAware
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.dispatchers.Broadcast

/**
 * A [Broadcast] component that dispatches [Event] messages to multiple branches simultaneously.
 *
 * [EventDispatcher] is used when multiple Spiders run in parallel in the topology:
 * it broadcasts each incoming [Event] to all registered output branches via [channelOuts],
 * using Meercat's [Broadcast] mechanism which sends to all branches and aggregates the backs.
 *
 * Output branches are registered dynamically at topology startup via [addBranch].
 * Dependency injection (Kodein) is used to resolve branches during topology construction.
 *
 * @see Broadcast
 * @see Component
 */
interface EventDispatcher: Component, DIAware, Broadcast {

    /**
     * Subscribes to the [Event] flow from [channelIn] and broadcasts each event
     * to all registered output branches.
     */
    suspend fun performEvents(){
        val flow = channelIn.getSendFlow(Event::class)
        val coroutineName = "${name}-performEvents"
        broadcast(coroutineName, flow)
    }

    /**
     * Registers a new output branch for event broadcasting.
     *
     * Called during topology construction to connect this dispatcher to a downstream channel.
     *
     * @param channel The [SuperChannel] of the branch to add.
     */
    fun addBranch(channel: SuperChannel){
        this.channelOuts.add(channel)
    }

    override suspend fun run() {
        performEvents()
    }
}