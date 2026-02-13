package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse

fun generateRequestResponse(sender: Controllable,
                            url: String = "an url",
                            status: Status = Status.OK,
                            errorInfos: ErrorInfo? = null): Pair<Request, DownloadingResponse> {
    val req = Request(sender, url)
    val resp = if(status == Status.OK){
        req.buildBack()
    }
    else{
        req.buildErrorBack(errorInfos!!, status)
    }

    return Pair(req, resp)
}