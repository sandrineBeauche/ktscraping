package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest




data class DownloadingResponse(
    override val request: DownloadingRequest,
    var type: ContentType = ContentType.HTML,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Response<DownloadingRequest>(request, status, errorInfos) {

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
}




