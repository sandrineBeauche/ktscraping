package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.RequestSender

abstract class DownloadingRequest(
    sender: RequestSender,
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
}

data class Request(
    override val sender: RequestSender,
    override var url: String
): DownloadingRequest(sender, url){
}

