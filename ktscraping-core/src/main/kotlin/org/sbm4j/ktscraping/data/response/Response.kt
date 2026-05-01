package org.sbm4j.ktscraping.data.response

import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import java.util.UUID

/**
 * Base back message returned by the Downloader branch in response to an [AbstractRequest].
 *
 * A [Response] is the back message associated with a download request, carrying the
 * processing status and any errors accumulated during the download. Concrete subclasses
 * add the actual downloaded content (e.g. HTML, JSON, images, etc.).
 *
 * @param send The original [AbstractRequest] this response is answering.
 * @param status Overall processing status, defaults to [Status.OK].
 * @param errorInfos List of errors accumulated during the download.
 *
 * @see AbstractRequest
 * @see DownloadingResponse
 */
abstract class Response(
    override val send: AbstractRequest,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()
): Back<AbstractRequest>{

    override val loggingLabel: String
        get() = super.loggingLabel

    override var channelableId: UUID = UUID.randomUUID()

}

class ResponseException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)