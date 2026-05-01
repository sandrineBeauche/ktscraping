package org.sbm4j.ktscraping.core.utils

import com.natpryce.hamkrest.*
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse

fun isOKEventItemAck(eventName: String): Matcher<EventBack>{
    return isA<EventBack>(
        allOf(
            has(EventBack::send, has(Event::eventName, equalTo(eventName))),
            has(EventBack::status, equalTo(Status.OK)),
            has(EventBack::errorInfos, isEmpty)
        )
    )
}

fun isOKStartItemAck() = isOKEventItemAck("start")
fun isOKEndItemAck() = isOKEventItemAck("end")

fun isEventItemAckWithErrors(eventName: String, status: Status, nbErrors: Int): Matcher<EventBack>{
    return isA<EventBack>(
        allOf(
            has(EventBack::send, has(Event::eventName, equalTo(eventName))),
            has(EventBack::status, equalTo(status)),
            has(EventBack::errorInfos, hasSize(equalTo(nbErrors)))
        )
    )
}

fun isStartItemAckWithErrors(status: Status, nbErrors: Int) = isEventItemAckWithErrors("start", status, nbErrors)
fun isEndItemAckWithErrors(status: Status, nbErrors: Int) = isEventItemAckWithErrors("end", status, nbErrors)


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
            has(DownloadingResponse::send, isDownloadingRequestWith(url)),
            has(DownloadingResponse::contents, equalTo(contents))
        )
    )
}

fun isDownloadingResponseWithErrors(url: String, status: Status, nbErrors: Int): Matcher<DownloadingResponse>{
    return isA<DownloadingResponse>(
        allOf(
            has(DownloadingResponse::send, isDownloadingRequestWith(url)),
            has(DownloadingResponse::status, equalTo(status)),
            has(DownloadingResponse::errorInfos, hasSize(equalTo(nbErrors)))
        )
    )
}

fun isOKEventBackWith(eventName: String): Matcher<EventBack>{
    return isA<EventBack>(
        allOf(
            has(EventBack::send, isA<Event>(
                has(Event::eventName, equalTo(eventName)))),
            has(EventBack::status, equalTo(Status.OK)),
            has(EventBack::errorInfos, isEmpty)
        )
    )
}

fun isEventResponseWithError(eventName: String, status: Status, nbErrors: Int): Matcher<EventBack>{
    return isA<EventBack>(
        allOf(
            has(EventBack::send, isA<Event>(
                has(Event::eventName, equalTo(eventName)))),
            has(EventBack::status, equalTo(status)),
            has(EventBack::errorInfos, hasSize(equalTo(nbErrors)))
        )
    )
}

fun isOkItemAck(item: Item): Matcher<ItemAck>{
    return isA<ItemAck>(
        allOf(
            has(ItemAck::send, sameInstance(item)),
            has(ItemAck::status, equalTo(Status.OK)),
            has(ItemAck::errorInfos, isEmpty)
        )
    )
}