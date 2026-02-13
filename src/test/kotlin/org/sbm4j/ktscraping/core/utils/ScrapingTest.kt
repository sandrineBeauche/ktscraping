package org.sbm4j.ktscraping.core.utils

import io.mockk.mockk
import kotlinx.coroutines.delay
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse

abstract class ScrapingTest {

    lateinit var inChannel: SuperChannel

    val sender: Controllable = mockk<Controllable>()

    open fun initChannels(){
        inChannel = SuperChannel()
    }

    open fun closeChannels(){
        inChannel.close()
    }

    open suspend fun doStartEvent(){
        logger.debug{"Do Start event"}
        val startEvent = StartEvent(sender)
        inChannel.sendSync<EventBack>(startEvent)
        logger.debug{"Start event done"}
    }

    open suspend fun doEndEvent(){
        logger.debug{"Do End event"}
        val endEvent = EndEvent(sender)
        inChannel.sendSync<EventBack>(endEvent)
        logger.debug{"End event done"}
    }

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

    fun generateRequestResponses(sender: Controllable,
                                 urls: List<String> = listOf("an url", "another url"),
                                 status: Status = Status.OK): Pair<List<Request>, List<DownloadingResponse>> {
        val reqs = urls.map { url -> Request(sender, url) }
        val resps = reqs.map { r -> DownloadingResponse(r, status = status) }
        return Pair(reqs, resps)
    }
}