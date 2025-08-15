package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.SuperChannel
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse

abstract class ScrapingTest {

    lateinit var inChannel: SuperChannel



    open fun initChannels(){
        inChannel = SuperChannel()
    }

    open fun closeChannels(){
        inChannel.close()
    }

    fun generateRequestResponse(sender: Controllable,
                                url: String = "an url",
                                status: Status = Status.OK): Pair<Request, DownloadingResponse> {
        val req = Request(sender, url)
        val resp = DownloadingResponse(req, status =status)
        return Pair(req, resp)
    }

    fun generateRequestResponses(sender: Controllable,
                                 urls: List<String> = listOf("an url", "another url"),
                                 status: Status = Status.OK): Pair<List<Request>, List<DownloadingResponse>> {
        val reqs = urls.map { url -> Request(sender, url) }
        val resps = reqs.map { r -> DownloadingResponse(r, status = status) }
        return Pair(reqs, resps)
    }
}