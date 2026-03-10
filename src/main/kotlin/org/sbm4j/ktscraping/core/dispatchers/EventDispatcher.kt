package org.sbm4j.ktscraping.core.dispatchers

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import org.sbm4j.meercat.components.Controllable
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.logger
import org.sbm4j.meercat.channels.sendSyncAll
import org.sbm4j.meercat.channels.Back
import org.sbm4j.meercat.channels.Send
import org.sbm4j.ktscraping.data.events.Event

interface SendPropagator: Controllable{
    val receivers: MutableList<SuperChannel>

    var channelIn: SuperChannel
}

interface SendPropagatorOne: SendPropagator {

    suspend fun <T: Send> propagateOne(coroutineName: String, flow: Flow<T>, selectChannelFunc: (T) -> SuperChannel){
        scope.launch(CoroutineName(coroutineName)) {
            flow.collect { send ->
                launch(CoroutineName("${coroutineName}-${send.name}")) {
                    val channel = selectChannelFunc(send)
                    val back = channel.sendSync<Back<*>>(send)
                    channelIn.send(back)
                }
            }
        }
    }
}

interface SendPropagatorAll: SendPropagator {

    suspend fun propagateAll(coroutineName: String, flow: Flow<Send>, message: String = ""){
        scope.launch(CoroutineName(coroutineName)) {
            flow.collect { send ->
                launch(CoroutineName("${coroutineName}-${send.name}")) {
                    logger.trace { "${name}: Received ${send.name} and dispatch it to all" }
                    val result = sendSyncAll(receivers, send)
                    logger.trace { "${name}: Received back for ${send.name} and forward it" }
                    channelIn.send(result)
                }
            }
        }
    }
}

interface EventDispatcher: Controllable, DIAware, SendPropagatorAll {

    suspend fun performEvents(){
        val flow = channelIn.getSendFlow(Event::class)
        val coroutineName = "${name}-performEvents"
        propagateAll(coroutineName, flow)
    }

    fun addBranch(channel: SuperChannel){
        this.receivers.add(channel)
    }

    override suspend fun run() {
        logger.info{ "Starting the dispatcher ${name}"}
        performEvents()
    }

    override suspend fun stop() {
        this.channelIn.close()
        for(receiver in receivers){
            receiver.close()
        }
    }
}