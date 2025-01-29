package org.sbm4j.ktscraping.requests

import org.sbm4j.ktscraping.core.ContentType

enum class Status{
    OK,
    UNAUTHORIEZD,
    NOT_FOUND,
    ERROR
}

data class Response(
    val request: AbstractRequest,
    val status: Status = Status.OK,
    var type: ContentType = ContentType.HTML): Channelable {

    val contents: MutableMap<String, Any> = mutableMapOf()

    fun isText(): Boolean{
        return when(type){
            ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> true
            ContentType.FILE, ContentType.IMAGE, ContentType.BITMAP_IMAGE -> false
        }
    }

    fun isByteArray(): Boolean{
        return when(type){
            ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> false
            ContentType.FILE, ContentType.IMAGE, ContentType.BITMAP_IMAGE -> true
        }
    }
}


class ResponseException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}