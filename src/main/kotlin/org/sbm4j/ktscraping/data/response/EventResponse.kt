package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.EventRequest

data class EventResponse(
    override val eventName: String,
    override val request: EventRequest,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Response<EventRequest>(request, status, errorInfos), EventBack