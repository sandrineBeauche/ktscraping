package org.sbm4j.ktscraping.core.utils

import com.natpryce.hamkrest.*
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse

fun isOKEventItemAck(eventName: String): Matcher<EventItemAck>{
    return isA<EventItemAck>(
        allOf(
            has(EventItemAck::eventName, equalTo(eventName)),
            has(EventItemAck::status, equalTo(Status.OK)),
            has(EventItemAck::errorInfos, isEmpty)
        )
    )
}

fun isOKStartItemAck() = isOKEventItemAck("start")
fun isOKEndItemAck() = isOKEventItemAck("end")

fun isDownloadingRequestWith(url: String): Matcher<DownloadingRequest>{
    return isA<DownloadingRequest>(
        allOf(
            has(DownloadingRequest::url, equalTo(url))
        )
    )
}

fun isDownloadingResponseWith(url: String, contents: MutableMap<String, Any>): Matcher<DownloadingResponse>{
    return isA<DownloadingResponse>(
        allOf(
            has(DownloadingResponse::request, isDownloadingRequestWith(url)),
            has(DownloadingResponse::contents, equalTo(contents))
        )
    )
}

fun isOKEventResponseWith(eventName: String): Matcher<EventResponse>{
    return isA<EventResponse>(
        allOf(
            has(EventResponse::eventName, equalTo(eventName)),
            has(EventResponse::status, equalTo(Status.OK)),
            has(EventResponse::errorInfos, isEmpty)
        )
    )
}

fun isEventResponseWithError(eventName: String, status: Status, nbErrors: Int): Matcher<EventResponse>{
    return isA<EventResponse>(
        allOf(
            has(EventResponse::eventName, equalTo(eventName)),
            has(EventResponse::status, equalTo(status)),
            has(EventResponse::errorInfos, hasSize(equalTo(nbErrors)))
        )
    )
}