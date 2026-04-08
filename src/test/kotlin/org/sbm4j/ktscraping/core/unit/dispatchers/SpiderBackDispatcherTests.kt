package org.sbm4j.ktscraping.core.unit.dispatchers

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.ktscraping.core.dispatchers.SpiderDispatcher
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.Stub
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.dispatchers.CombinatorTester
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.test.Test


class BackDispatcherTests: CombinatorTester<SpiderDispatcher>(){

    val di: DI = mockk<DI>()

    val nbSenders: Int = 3

    var senders: List<SendSource> = List(nbSenders){mockk<SendSource>()}

    override val nbChannelsIns: Int = 3

    override fun buildNode(): SpiderDispatcher {
        val result = SpiderDispatcher(di = di)
        result.channelsIns.addAll(channelsIns)
        result.channelOut = channelOut
        return result
    }

    override fun buildStub(channel: SuperChannel): Stub {
        return ComponentStub("stub", channel)
    }

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




    @Test
    fun `send request`() = TestScope().runTest {
        val url = "an url"
        val request = Request(senders[0], url)

        val response = channelsIns[0].sendSync<DownloadingResponse>(request)
        logger.debug { "Received response: $response" }

        coVerify(exactly = 1) { stub.processSend(any()) }
    }

    @Test
    fun `send events barrier`() = TestScope().runTest {
        coroutineScope {
            channelsIns.forEachIndexed { index, channel ->
                launch {
                    val event = StartEvent(senders[index])
                    val resp = channel.sendSync<EventBack>(event)
                }
            }
        }

        coVerify(exactly = 1) { stub.processSend(any()) }
    }

    @Test
    fun `send internal`() = TestScope().runTest {
        val error = ErrorInfo(Exception("an exception"), senders[0], ErrorLevel.MAJOR)
        val internal = ErrorInternal(error, senders[0])

        val cs = stub as ComponentStub

        channelsIns[0].send(internal)
        cs.internalSendLatch.await()

        coVerify(exactly = 1) { (stub as ComponentStub).processInternal(any()) }
    }
}