package org.sbm4j.ktscraping.core.unit

import io.mockk.coVerify
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EventRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import kotlin.test.Test
import kotlin.test.assertEquals


class MiddlewareTest: AbstractMiddlewareTester() {

    val url = "Another url"

    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        return object: AbstractMiddleware(middlewareName){
            override suspend fun processResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
                return true
            }

            override suspend fun processDataRequest(request: DownloadingRequest): Any? {
                request.url = url
                return request
            }

            override suspend fun preStart(event: Event): Job? {
                logger.info{ "${name}: inside pre start event"}
                return super.preStart(event)
            }

            override suspend fun postStart(event: EventResponse) {
                logger.info{ "${name}: inside post start event"}
                super.postStart(event)
            }
        }
    }

    @Test
    fun testForwardingRequestAndResponse() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender)

        withMiddleware {
            inChannel.send(req)
            val receivedReq = forwardInChannel.receive() as DownloadingRequest

            assertEquals(req.name, receivedReq.name)
            assertEquals(url, receivedReq.url)

            outChannel.send(resp)
            val receivedResp = forwardOutChannel.receive()
        }

        coVerify { middleware.processDataRequest(req)}
        coVerify { middleware.processResponse(resp, req)}
    }

    @Test
    fun testStartEvent() = TestScope().runTest {
        val startRequest = StartRequest(sender)
        val startResponse = EventResponse(startRequest)

        withMiddleware {
            inChannel.send(startRequest)
            val receivedReq = forwardInChannel.receive() as EventRequest
            logger.info{"received forwarded start event"}

            assertEquals("start", receivedReq.eventName)

            logger.info{ "send response for start event"}
            outChannel.send(startResponse)
            val receivedResp = forwardOutChannel.receive()
        }

        coVerify { middleware.preStart(startRequest)}
        coVerify { middleware.postStart(startResponse) }
    }
}