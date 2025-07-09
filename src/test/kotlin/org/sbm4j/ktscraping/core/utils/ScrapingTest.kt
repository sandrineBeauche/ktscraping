package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Status

abstract class ScrapingTest<IN, OUT> {

    lateinit var inChannel: Channel<IN>

    lateinit var outChannel: Channel<OUT>


    open fun initChannels(){
        inChannel = Channel<IN>(Channel.UNLIMITED)
        outChannel = Channel<OUT>(Channel.UNLIMITED)
    }

    open fun closeChannels(){
        inChannel.close()
        outChannel.close()
    }

    fun generateRequestResponse(sender: RequestSender,
                                url: String = "an url",
                                status: Status = Status.OK): Pair<Request, DownloadingResponse> {
        val req = Request(sender, url)
        val resp = DownloadingResponse(req, status =status)
        return Pair<Request, DownloadingResponse>(req, resp)
    }

    fun generateRequestResponses(sender: RequestSender,
                                 urls: List<String> = listOf("an url", "another url"),
                                 status: Status = Status.OK): Pair<List<Request>, List<DownloadingResponse>> {
        val reqs = urls.map { url -> Request(sender, url) }
        val resps = reqs.map { r -> DownloadingResponse(r, status = status) }
        return Pair(reqs, resps)
    }
}