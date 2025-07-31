package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.Channelable
import java.util.concurrent.atomic.AtomicInteger


open class AbstractRequest(open val sender: RequestSender): Channelable {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val reqId = lastId.getAndIncrement()

    val name = "Request-${reqId}"

    val parameters: MutableMap<String, Any> = mutableMapOf()

}