package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.components.DownloaderMiddleware
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderMiddlewareTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.core.utils.isDownloadingResponseWithErrors
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import kotlin.test.Test

class TestingDownloaderMiddleware : DownloaderMiddleware("middleware"){
    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        request.url = "another url"
        return true
    }
}

class DownloaderMiddlewareTest: AbstractDownloaderMiddlewareTester<DownloaderMiddleware>() {

    override fun buildNode(): DownloaderMiddleware {
        val result = TestingDownloaderMiddleware()
        result.inChannel = inChannel
        result.outChannel = outChannel
        return result
    }


    @Test
    fun `forward request and response`() = testScope.runTest {
        val url1 = "an url"
        val url2 = "another url"

        val cs = (stub as ComponentStub)
        val data = mutableMapOf<String, Any>("result" to "1")
        cs.downloadingResponses[url2] = Pair(ContentType.STRING, data)

        val req = Request(sender, url1)
        lateinit var resp: DownloadingResponse

        withConsumer {
            resp = inChannel.sendSync<DownloadingResponse>(req)
        }

        val captured = getReceivedSend(cs)
        assertThat(captured, hasSize(equalTo(3)))
        assertThat(captured[1], isA<Request>(
            has(Request::url, equalTo(url2))
        ))
        assertThat(resp, isDownloadingResponseWith(url2, data))
    }


    @Test
    fun `forward request and error response`() = testScope.runTest {
        val url1 = "an url"
        val url2 = "another url"

        val cs = (stub as ComponentStub)
        respondWithError(
            cs,
            {it is DownloadingRequest && it.url == url2},
            "an error occurs while downloading",
            ErrorLevel.MAJOR
            )

        val req = Request(sender, url1)
        lateinit var resp: DownloadingResponse

        withConsumer {
            resp = inChannel.sendSync<DownloadingResponse>(req)
        }

        val captured = getReceivedSend(cs)
        assertThat(captured, hasSize(equalTo(3)))
        assertThat(captured[1], isA<Request>(
            has(Request::url, equalTo(url2))
        ))
        assertThat(resp, isDownloadingResponseWithErrors(url2, Status.ERROR, 1))
    }



}