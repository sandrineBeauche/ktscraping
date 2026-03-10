package org.sbm4j.ktscraping.core.unit.dispatchers

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.channels.sendSyncAll
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.core.dispatchers.SpiderDispatcher
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.components.SendSource
import kotlin.test.BeforeTest
import kotlin.test.Test


class BackDispatcherTests{

    lateinit var outChannel: SuperChannel

    lateinit var inChannels: List<SuperChannel>

    lateinit var senders: List<SendSource>

    val di: DI = mockk<DI>()

    lateinit var dispatcher: SpiderDispatcher

    val nbSenders: Int = 3

    @BeforeTest
    fun setUp(){
        dispatcher = SpiderDispatcher(di = di)

        outChannel = SuperChannel()
        inChannels = List(nbSenders){SuperChannel()}

        dispatcher.channelOut = outChannel
        dispatcher.channelsIns.addAll(inChannels)

        senders = List(nbSenders){mockk<SendSource>()}
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

    suspend fun doStartEvent(){
        logger.debug{"Do Start event"}

        val startEvents = senders.map{StartEvent(it)}
        val sends = inChannels.zip(startEvents).toMap()
        val result = sendSyncAll(sends)
        logger.debug{"Start event done: $result"}
    }

    suspend fun doEndEvent(){
        logger.debug{"Do End event"}
        val endEvent = senders.map{EndEvent(it)}
        val sends = inChannels.zip(endEvent).toMap()
        val result = sendSyncAll(sends)
        logger.debug{"End event done: $result"}
    }

    fun initChannels(parentScope: CoroutineScope){
        outChannel.init(parentScope)
        inChannels.forEach {it.init(parentScope)}
    }

    suspend fun closeChannels(){
        logger.debug{"Close channels"}
        outChannel.close()
        inChannels.forEach {it.close()}
        logger.debug { "Close channels finished" }
    }

    suspend fun withDispatcher(nbMessages: Int = 1, func: suspend BackDispatcherTests.() -> Unit){
        coroutineScope {
            initChannels(this)

            launch{
                dispatcher.start(this)
                doStartEvent()
                func()
                doEndEvent()
                dispatcher.stop()
                closeChannels()
            }
            launch{
                outChannel.getSendFlow().take(nbMessages + 2).collect{ send ->
                    val back = send.buildBack()
                    outChannel.send(back)
                }
                logger.debug { "Finished with $nbMessages messages" }
            }
        }
    }

    @Test
    fun testSendEvent() = TestScope().runTest {
        val (req1, resp1) = generateRequestResponse(senders[0])

        withDispatcher {
            val response = inChannels[0].sendSync<DownloadingResponse>(req1)
            logger.debug{"Received response: $response"}
        }
    }
}