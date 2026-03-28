package org.sbm4j.meercat.nodes

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer

abstract class AbstractSinkNode(
    override val name: String
): AbstractProcessingNode(), SendConsumer {

    override lateinit var inChannel: SuperChannel

    override suspend fun sendPostProcess(send: Send, result: Any) {
        if(result is Back<*>){
            inChannel.send(result)
        }
    }
}