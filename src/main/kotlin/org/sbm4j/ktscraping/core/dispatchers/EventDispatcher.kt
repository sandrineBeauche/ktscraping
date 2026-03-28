package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DIAware
import org.sbm4j.ktscraping.core.components.Component
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.dispatchers.Broadcast


interface EventDispatcher: Component, DIAware, Broadcast {

    suspend fun performEvents(){
        val flow = channelIn.getSendFlow(Event::class)
        val coroutineName = "${name}-performEvents"
        broadcast(coroutineName, flow)
    }

    fun addBranch(channel: SuperChannel){
        this.channelOuts.add(channel)
    }

    override suspend fun run() {
        performEvents()
    }

    override suspend fun stop() {
        this.channelIn.close()
        for(receiver in channelOuts){
            receiver.close()
        }
    }
}