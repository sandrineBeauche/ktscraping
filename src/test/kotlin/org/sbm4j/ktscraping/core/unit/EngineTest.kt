package org.sbm4j.ktscraping.core.unit

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractEngine
import org.sbm4j.ktscraping.core.ChannelFactory
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.dsl.DataItemTest
import org.sbm4j.ktscraping.core.dsl.TestingCrawlerResult
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.isEventResponseWithError
import org.sbm4j.ktscraping.core.utils.isOKEventResponseWith
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.EventItem
import org.sbm4j.ktscraping.data.item.EventItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestingEngine(
    channelFactory: ChannelFactory,
) : AbstractEngine(channelFactory) {
    override fun computeResult(): CrawlerResult {
        return TestingCrawlerResult()
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }

}

class EngineTest {

    val scope = TestScope()

    val sender: Controllable = mockk<Controllable>()

    lateinit var channelFactory : ChannelFactory

    @BeforeTest
    fun setUp(){
        channelFactory = ChannelFactory()
    }

    suspend fun withEngine(func: suspend EngineTest.() -> Unit){
        val engine = TestingEngine(channelFactory)
        coroutineScope {
            launch{
                engine.start(this)
            }
            launch{
                logger.info{"Starting interacting with engine"}

                func()

                engine.stop()
                channelFactory.closeChannels()
            }
        }
    }


    @Test
    fun testEngineSendRequest() = TestScope().runTest {
        val request1 = Request(sender, "une url")

        withEngine {
            channelFactory.spiderRequestChannel.send(request1)
            val req = channelFactory.downloaderRequestChannel.receive()

            logger.info { "Received request on downloader branch: ${req}" }
        }
    }

    @Test
    fun testEngineSendResponse() = TestScope().runTest {
        val request1 = Request(sender, "une url")
        val response = DownloadingResponse(request1, ContentType.NOTHING, Status.OK)

        withEngine {
            channelFactory.downloaderResponseChannel.send(response)
            val resp = channelFactory.spiderResponseChannel.receive()

            logger.info { "Received response on downloader branch: ${resp}" }
        }
    }

    @Test
    fun testEngineSendItem() = TestScope().runTest {
        val data = DataItemTest("value1", "req1")
        val item = ObjectDataItem<DataItemTest>(data, data::class)

        withEngine {
            channelFactory.spiderItemChannel.send(item)
            val it = channelFactory.itemChannel.receive()

            logger.info { "Received item on item branch: ${it}" }
        }
    }

    @Test
    fun testEngineSendStartEvent() = TestScope().runTest {
        val startReq = StartRequest(sender)

        withEngine {
            channelFactory.spiderRequestChannel.send(startReq)

            coroutineScope {
                launch{
                    val req = channelFactory.downloaderRequestChannel.receive() as EventRequest
                    logger.debug{ "Received an event request and answers it: $req" }
                    val resp = EventResponse(req.eventName, startReq)
                    channelFactory.downloaderResponseChannel.send(resp)
                }
                launch {
                    val item = channelFactory.itemChannel.receive() as EventItem
                    val itemAck = EventItemAck(item.channelableId, item.eventName)
                    logger.debug{ "Received an event item and send back a ack: $item "}
                    channelFactory.itemAckChannel.send(itemAck)
                }
            }

            val resp = channelFactory.spiderResponseChannel.receive() as EventResponse
            logger.info{ "received the final response: $resp" }

            assertThat(resp, isOKEventResponseWith("start"))
        }
    }

    @Test
    fun testEngineSendStartEventWithError1() = TestScope().runTest {
        val startReq = StartRequest(sender)

        withEngine {
            channelFactory.spiderRequestChannel.send(startReq)

            coroutineScope {
                launch{
                    val req = channelFactory.downloaderRequestChannel.receive() as EventRequest
                    logger.debug{ "Received an event request and answers it: $req" }
                    val resp = EventResponse(req.eventName, startReq)
                    channelFactory.downloaderResponseChannel.send(resp)
                }
                launch {
                    val item = channelFactory.itemChannel.receive() as EventItem
                    val error = ErrorInfo(Exception(),sender, ErrorLevel.MAJOR, "an error message")
                    val itemAck = item.generateAck(Status.ERROR, mutableListOf(error))
                    logger.debug{ "Received an event item and send back a ack with errors: $item "}
                    channelFactory.itemAckChannel.send(itemAck)
                }
            }

            val resp = channelFactory.spiderResponseChannel.receive() as EventResponse
            logger.info{ "received the final response: $resp" }

            assertThat(resp, isEventResponseWithError("start", Status.ERROR, 1))
        }
    }
}