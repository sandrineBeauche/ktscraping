package org.sbm4j.ktscraping.core.unit.components

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class MiddlewareTest: AbstractMiddlewareTester() {

    val url = "Another url"

    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        return object: AbstractMiddleware(middlewareName){

            override suspend fun processDataRequest(request: DownloadingRequest): Any? {
                request.url = url
                return true
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

        val req = Request(sender, "an url")
        req.parameters[RESP_STATUS] = Status.OK
        req.parameters[RESP_CONTENTS] = mutableMapOf("value" to 1)

        withMiddleware {
            val resp = inChannel.sendSync<DownloadingResponse>(req)

            assertEquals(req.name, resp.send.name)
            assertEquals(url, resp.send.url)
        }
    }

}