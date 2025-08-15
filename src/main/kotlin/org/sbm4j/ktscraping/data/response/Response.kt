package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import java.util.UUID


abstract class Response<T: AbstractRequest>(
    override val send: T,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Back<T>{

    override var channelableId: UUID = UUID.randomUUID()

}

class ResponseException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)