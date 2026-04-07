package org.sbm4j.ktscraping.middleware


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.assertThrows
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderMiddlewareTester
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class SchedulerMiddlewareTests: AbstractDownloaderMiddlewareTester<SchedulerMiddleware>() {

    override fun buildNode(): SchedulerMiddleware {
        val result = SchedulerMiddleware("scheduler middleware")
        result.nbConnexions = 1
        return result
    }

    @Test
    fun testSchedulerMiddleware1() = TestScope().runTest{
        val request = Request(sender, "http://server1/add1")
        val response = DownloadingResponse(request)

        lateinit var req: Request
        lateinit var resp: DownloadingResponse

        withConsumer {
            inChannel.send(request)
            req = outChannel.channel.receive() as Request

            outChannel.send(response)
            resp = outChannel.channel.receive() as DownloadingResponse
        }

        assertThat(req, sameInstance(request))
        assertThat(resp, sameInstance(response))
    }

    @Test
    fun testSchedulerMiddleware2() = TestScope().runTest{
        val request1 = Request(sender, "http://server1/add1")
        val request2 = Request(sender, "http://server1/add1")
        val response1 = DownloadingResponse(request1)
        val response2 = DownloadingResponse(request2)

        lateinit var req1: Request
        lateinit var resp1: DownloadingResponse
        lateinit var req2: Request
        lateinit var resp2: DownloadingResponse

        withConsumer {
            inChannel.send(request1)
            inChannel.send(request2)

            req1 = outChannel.channel.receive() as Request
            outChannel.send(response1)
            resp1 = outChannel.channel.receive() as DownloadingResponse

            req2 = outChannel.channel.receive() as Request
            outChannel.send(response2)
            resp2 = outChannel.channel.receive() as DownloadingResponse
        }

        assertThat(req1, sameInstance(request1))
        assertThat(resp1, sameInstance(response1))

        assertThat(req2, sameInstance(request2))
        assertThat(resp2, sameInstance(response2))
    }

    @Test
    fun testSchedulerMiddleware3() = TestScope().runTest {
        val request1 = Request(sender, "http://server1/add1")
        val request2 = Request(sender, "http://server1/add1")
        val response1 = DownloadingResponse(request1)
        val response2 = DownloadingResponse(request2)

        assertThrows<TimeoutCancellationException> {
            withTimeout(5000L.milliseconds) {
                withConsumer {
                    inChannel.send(request1)
                    inChannel.send(request2)

                    outChannel.channel.receive() as Request
                    outChannel.channel.receive() as Request

                    outChannel.send(response1)
                    outChannel.channel.receive()

                    outChannel.send(response2)
                    outChannel.channel.receive()
                }
            }
        }
    }
}