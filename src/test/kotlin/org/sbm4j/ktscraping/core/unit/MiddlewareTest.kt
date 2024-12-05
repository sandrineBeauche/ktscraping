package org.sbm4j.ktscraping.core.unit

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

    override fun buildMiddleware(sc: CoroutineScope, middlewareName: String): AbstractMiddleware {
        return object: AbstractMiddleware(sc, middlewareName){
            override fun processResponse(response: Response): Boolean {
                return true
            }

            override fun processRequest(request: AbstractRequest): Any? {
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

        verify { middleware.processRequest(req)}
        verify { middleware.processResponse(resp)}
    }
}