package org.sbm4j.ktscraping.requests

enum class Status{
    OK,
    UNAUTHORIEZD,
    NOT_FOUND,
    ERROR
}

data class Response(val request: Request, val status: Status = Status.OK): Channelable {

    val contents: MutableMap<String, Any> = mutableMapOf()
}