package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

abstract class DownloadingRequest(
    sender: SendSource,
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

    override fun buildErrorBack(infos: ErrorInfo, status: Status): DownloadingResponse {
        return DownloadingResponse(this, ContentType.NOTHING,
            status, mutableListOf(infos), "${this.name}-Response")
    }

    override fun buildBack(): DownloadingResponse {
        return DownloadingResponse(this, ContentType.NOTHING, name = "${this.name}-Response")
    }

    abstract override fun clone(): DownloadingRequest

    override fun getKeyBarrier(): String {
        return this.url
    }

    override fun toURIString(): String {
        return this.url
    }
}

data class Request(
    override var sender: SendSource,
    override var url: String
): DownloadingRequest(sender, url){

    override fun clone(): Request {
        return this.copy()
    }
}

