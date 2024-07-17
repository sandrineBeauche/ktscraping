package org.sbm4j.ktscraping.requests

import org.sbm4j.ktscraping.core.RequestSender
import java.util.concurrent.atomic.AtomicInteger

data class Request(val sender: RequestSender, var url: String ): Channelable {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val reqId = lastId.getAndIncrement()

    val name = "Request-${reqId}"

    val parameters: MutableMap<String, Any> = mutableMapOf()

    fun extractServerFromUrl(): String{
        val start = url.indexOf("://")
        val end = url.indexOf("/", start + 3)
        return url.substring(start + 3, end)
    }
}