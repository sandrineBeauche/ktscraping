package org.sbm4j.ktscraping.requests

import org.sbm4j.ktscraping.core.RequestSender
import java.util.concurrent.atomic.AtomicInteger

data class Request(val sender: RequestSender, var url: String ): Channelable {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val reqId = lastId.getAndIncrement()

    val name = "Request-${reqId}"
}