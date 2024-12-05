package org.sbm4j.ktscraping.requests

import com.microsoft.playwright.Page
import org.sbm4j.ktscraping.core.RequestSender
import java.util.concurrent.atomic.AtomicInteger

open class AbstractRequest(open val sender: RequestSender, open var url: String ): Channelable {
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

data class Request(
    override val sender: RequestSender,
    override var url: String
): AbstractRequest(sender, url)

class PlaywrightRequest(
    override val sender: RequestSender,
    override var url: String,
    func: Page.() -> Unit
): AbstractRequest(sender, url){
    init {
        parameters["playwright"] = func
    }
}