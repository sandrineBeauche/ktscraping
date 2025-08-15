package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Send
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger


abstract class AbstractRequest(override var sender: Controllable): Send {
    companion object {
        val lastId = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    override val name = "Request-${lastId.getAndIncrement()}"

    val parameters: MutableMap<String, Any> = mutableMapOf()

}