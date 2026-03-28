package org.sbm4j.meercat.exemples

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.SimpleBack
import org.sbm4j.meercat.data.SimpleSend
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.sendProcessors.AbstractInitiator
import org.sbm4j.meercat.nodes.sendProcessors.Initiator
import org.sbm4j.meercat.nodes.sendProcessors.NodeStatus
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias StringSend = SimpleSend<String>
typealias StringBack = SimpleBack<String>

class Source(
    override val name: String
): AbstractInitiator(){

    override suspend fun run() {
        val message = StringSend("coucou", this)
        val response = sendSync(message)
    }
}

class Middle(
    name: String
): AbstractMiddleNode(name){
    override lateinit var scope: CoroutineScope

    suspend fun performSend(message: StringSend): Any{
        message.value += " Comment ça va?"
        return message
    }

    suspend fun performBack(response: StringBack): Any{
        return response
    }

    override suspend fun run() {
        val sendClazz = StringSend::class
        val flow = inChannel.getSendFlow(sendClazz)
        performSends(sendClazz, flow, ::performSend)

        val backClazz = StringBack::class
        val flowBack = outChannel.getBackFlow(backClazz)
        receiveBacks(backClazz, flowBack, ::performBack)
    }
}

class Sink(name: String): AbstractSinkNode(name){

    suspend fun performSend(message: StringSend): Any{
        return message.buildBack()
    }

    override suspend fun run() {
        val sendClazz = StringSend::class
        val flow = inChannel.getSendFlow(sendClazz)
        performSends(sendClazz, flow, ::performSend)
    }

}

fun main(){

}