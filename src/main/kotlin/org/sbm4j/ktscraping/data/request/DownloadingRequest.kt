package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.response.DownloadingResponse

abstract class DownloadingRequest(
    sender: Controllable,
    open var url: String
): AbstractRequest(sender){

    companion object{
        val rawExtensions: List<String> = listOf("png", "bmp", "jpg", "jpeg")
    }

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
        return "url:${url}"
    }

    override fun buildErrorBack(infos: ErrorInfo): Back<*> {
        return DownloadingResponse(this, ContentType.NOTHING,
            Status.ERROR, mutableListOf(infos))
    }
}

data class Request(
    override var sender: Controllable,
    override var url: String
): DownloadingRequest(sender, url){
}

