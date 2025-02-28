package org.sbm4j.ktscraping.requests

import org.sbm4j.ktscraping.core.RequestSender
import java.util.concurrent.atomic.AtomicInteger


open class AbstractRequest(open val sender: RequestSender, open var url: String ): Channelable {
    companion object {
        val lastId = AtomicInteger(0)
        val rawExtensions: List<String> = listOf("png", "bmp", "jpg", "jpeg")
    }

    val reqId = lastId.getAndIncrement()

    val name = "Request-${reqId}"

    val parameters: MutableMap<String, Any> = mutableMapOf()

    fun extractServerFromUrl(): String{
        val start = url.indexOf("://")
        val end = url.indexOf("/", start + 3)
        return if(end > 0){
            url.substring(start + 3, end)
        } else{
            url.substring(start + 3)
        }
    }

    fun isRawImage(): Boolean{
        val extension = url.split(".").last()
        return rawExtensions.contains(extension)
    }

    open fun toCacheKey(): String {
        val stringParams = "[${parameters}]"
        return "url:${url}${stringParams}"
    }
}

data class Request(
    override val sender: RequestSender,
    override var url: String
): AbstractRequest(sender, url){

    override fun toCacheKey(): String {
        return "url:${url}"
    }
}
