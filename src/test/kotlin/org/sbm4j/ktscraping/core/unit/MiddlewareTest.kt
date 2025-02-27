package org.sbm4j.ktscraping.core.unit

import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.Test
import kotlin.test.assertEquals


class MiddlewareTest: AbstractMiddlewareTester() {

    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        return object: AbstractMiddleware(middlewareName){
            override suspend fun processResponse(response: Response): Boolean {
                return true
            }

            override suspend fun processRequest(request: AbstractRequest): Any? {
                request.url = "Another url"
                return request
            }

        }
    }

    @Test
    fun testFollowingRequestAndResponse() = TestScope().runTest {
        val (req, resp) = generateRequestResponse(sender)

        withMiddleware {
            inChannel.send(req)
            val receivedReq = followInChannel.receive()

            assertEquals(req.name, receivedReq.name)
            assertEquals("Another url", receivedReq.url)

            outChannel.send(resp)
            val receivedResp = followOutChannel.receive()
        }

        coVerify { middleware.processRequest(req)}
        coVerify { middleware.processResponse(resp)}
    }
}