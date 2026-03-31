package org.sbm4j.meercat.exemples

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.meercat.data.StringSend
import org.sbm4j.meercat.data.StringBack
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.AbstractSinkNode
import org.sbm4j.meercat.nodes.sendProcessors.AbstractInitiator


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