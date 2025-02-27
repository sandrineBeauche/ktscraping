package org.sbm4j.ktscraping.middleware


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.assertThrows
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.Test

class SchedulerMiddlewareTests: AbstractMiddlewareTester() {

    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        val result = SchedulerMiddleware(middlewareName)
        result.nbConnexions = 1
        return result
    }

    @Test
    fun testSchedulerMiddleware1() = TestScope().runTest{
        val request = Request(sender, "http://server1/add1")
        val response = Response(request)

        lateinit var req: Request
        lateinit var resp: Response

        withMiddleware {
            inChannel.send(request)
            req = followInChannel.receive() as Request

            outChannel.send(response)
            resp = followOutChannel.receive()
        }

        assertThat(req, sameInstance(request))
        assertThat(resp, sameInstance(response))
    }

    @Test
    fun testSchedulerMiddleware2() = TestScope().runTest{
        val request1 = Request(sender, "http://server1/add1")
        val request2 = Request(sender, "http://server1/add1")
        val response1 = Response(request1)
        val response2 = Response(request2)

        lateinit var req1: Request
        lateinit var resp1: Response
        lateinit var req2: Request
        lateinit var resp2: Response

        withMiddleware {
            inChannel.send(request1)
            inChannel.send(request2)

            req1 = followInChannel.receive() as Request
            outChannel.send(response1)
            resp1 = followOutChannel.receive()

            req2 = followInChannel.receive() as Request
            outChannel.send(response2)
            resp2 = followOutChannel.receive()
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
        val response1 = Response(request1)
        val response2 = Response(request2)

        assertThrows<TimeoutCancellationException> {
            withTimeout(5000L) {
                withMiddleware {
                    inChannel.send(request1)
                    inChannel.send(request2)

                    followInChannel.receive() as Request
                    followInChannel.receive() as Request

                    outChannel.send(response1)
                    followOutChannel.receive()

                    outChannel.send(response2)
                    followOutChannel.receive()
                }
            }
        }
    }
}