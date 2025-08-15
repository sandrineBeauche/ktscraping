package org.sbm4j.ktscraping.core.unit

import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.EventJobResult
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.EventBack
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test
import kotlin.test.assertEquals


class MiddlewareTest: AbstractMiddlewareTester() {

    val url = "Another url"

    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        return object: AbstractMiddleware(middlewareName){
            override suspend fun processDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
                return true
            }

            override suspend fun processDataRequest(request: DownloadingRequest): Any? {
                request.url = url
                return request
            }

            override suspend fun preStart(event: Event): EventJobResult? {
                logger.info{ "${name}: inside pre start event"}
                return super.preStart(event)
            }

            override suspend fun postStart(event: EventBack) {
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
            val receivedReq = outChannel.receive() as DownloadingRequest

            assertEquals(req.name, receivedReq.name)
            assertEquals(url, receivedReq.url)

            outChannel.send(resp)
            forwardOutChannel.receive()
        }

        coVerify { middleware.processDataRequest(req)}
        coVerify { middleware.processDownloadingResponse(resp, req)}
    }

    @Test
    fun testStartEvent() = TestScope().runTest {
        withMiddleware {
            logger.info{"middleware does something..."}
        }

        coVerify { middleware.preStart(any())}
        coVerify { middleware.postStart(any()) }
        coVerify { middleware.preEnd(any())}
        coVerify { middleware.postEnd(any()) }
    }
}