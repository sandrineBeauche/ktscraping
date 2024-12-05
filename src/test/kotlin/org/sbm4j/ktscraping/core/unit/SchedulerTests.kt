package org.sbm4j.ktscraping.core.unit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.CrawlerConfiguration
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.Scheduler
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import kotlin.test.BeforeTest
import kotlin.test.Test

class SchedulerTests {

    lateinit var configuration: CrawlerConfiguration

    lateinit var requests: Channel<AbstractRequest>

    val sender: RequestSender = mockk<RequestSender>()

    lateinit var scheduler: Scheduler

    @BeforeTest
    fun setUp(){
        configuration = CrawlerConfiguration()
        requests = Channel(Channel.UNLIMITED)
    }

    suspend fun performReceive(scheduler: Scheduler): AbstractRequest?{
        val result = requests.receive()
        scheduler.receivedResponse()
        return result
    }

    //@Test
    fun test1Server() = TestScope().runTest{
        val sendingReq = Request(sender, "http://server1/add1")
        var receivedReq: AbstractRequest? = null

        coroutineScope {
            scheduler = Scheduler(this, configuration)
            scheduler.requestOut = requests
            scheduler.start()

            launch{
                scheduler.submitRequest(sendingReq)
            }
            launch{
                receivedReq = performReceive(scheduler)
                scheduler.stop()
            }
        }

        assertThat(receivedReq, sameInstance(sendingReq))
    }

    @Test
    fun test2Server() = TestScope().runTest {
        configuration.nbConnexions = 1
        val sendingReq1 = Request(sender, "http://server1/add1")
        val sendingReq2 = Request(sender, "http://server1/add2")

        var receivedReq1: AbstractRequest? = null
        var receivedReq2: AbstractRequest? = null

        coroutineScope {
            scheduler = Scheduler(this, configuration)
            scheduler.requestOut = requests
            scheduler.start()

            launch {
                scheduler.submitRequest(sendingReq1)
                scheduler.submitRequest(sendingReq2)
            }

            launch{
                receivedReq1 = performReceive(scheduler)
                receivedReq2 = performReceive(scheduler)
                scheduler.stop()
            }
        }
    }
}