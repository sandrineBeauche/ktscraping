package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.ktscraping.data.request.DownloadingRequest


data class DownloadingResponse(
    override val send: DownloadingRequest,
    var type: ContentType = ContentType.HTML,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "${send.name}-DownloadingResponse"
): Response(send, status, errorInfos) {

    val contents: MutableMap<String, Any> = mutableMapOf()

    fun isText(): Boolean{
        return when(type){
            ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> true
            ContentType.FILE, ContentType.IMAGE, ContentType.BITMAP_IMAGE, ContentType.NOTHING -> false
        }
    }

    fun isByteArray(): Boolean{
        return when(type){
            ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML, ContentType.NOTHING -> false
            ContentType.FILE, ContentType.IMAGE, ContentType.BITMAP_IMAGE -> true
        }
    }

    override fun clone(): DownloadingResponse {
        return this.copy()
    }
}




