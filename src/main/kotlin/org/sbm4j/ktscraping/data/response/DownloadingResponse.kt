package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest

enum class Status{
    OK,
    UNAUTHORIEZD,
    NOT_FOUND,
    ERROR
}

abstract class Response<T: AbstractRequest>(
    open val request: T,
    open val status: Status = Status.OK,
    open val errorInfos: ErrorInfo? = null
): Channelable{}


data class DownloadingResponse(
    override val request: DownloadingRequest,
    var type: ContentType = ContentType.HTML,
    override  val status: Status = Status.OK,
    override val errorInfos: ErrorInfo? = null
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

data class EventResponse(
    override val request: EventRequest,
    override  val status: Status = Status.OK,
    override val errorInfos: ErrorInfo? = null
): Response<EventRequest>(request, status, errorInfos)


class ResponseException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)