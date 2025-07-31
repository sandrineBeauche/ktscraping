package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest



abstract class Response<T: AbstractRequest>(
    open val request: T,
    status: Status = Status.OK,
    errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Back

class ResponseException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)