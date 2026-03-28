package org.sbm4j.ktscraping.core.utils

import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

fun generateRequestResponse(sender: SendSource,
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