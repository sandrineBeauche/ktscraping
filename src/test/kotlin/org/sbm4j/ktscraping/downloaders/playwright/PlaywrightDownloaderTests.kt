package org.sbm4j.ktscraping.downloaders.playwright

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightRequest
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlaywrightDownloaderTests: AbstractDownloaderTester<PlaywrightDownloader>() {

    override fun buildNode(): PlaywrightDownloader {
        val result = PlaywrightDownloader("playwright dowloader")
        result.headless = true
        result.inChannel = inChannel
        return result
    }

    @Test
    fun testSimpleDownload() = TestScope().runTest {
        val request = Request(sender, "https://playwright.dev")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertNotNull(response)
    }

    @Test
    fun testSVGImageDownload() = TestScope().runTest {
        val request = Request(sender, "https://www.iana.org/_img/2022/iana-logo-header.svg")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertNotNull(response)
    }

    @Test
    fun testPNGImageDownload() = TestScope().runTest {
        val request = Request(sender, "https://fr.wikipedia.org/static/images/icons/wikipedia.png")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertNotNull(response)
    }



    @Test
    fun testMultipleNamed() = TestScope().runTest {
        val request1 = PlaywrightRequest(sender, "https://playwright.dev"){
            waitForTimeout(3000.0)
        }

        val request2 = PlaywrightRequest(sender, "https://playwright.dev"){
            waitForTimeout(3000.0)
        }

        lateinit var response1: DownloadingResponse
        lateinit var response2: DownloadingResponse

        request1.parameters["contextName"] = "context1"
        request2.parameters["contextName"] = "context1"

        withConsumer {
            val job1 = launch {
                response1 = inChannel.sendSync<DownloadingResponse>(request1)
            }
            val job2 = launch {
                response2 = inChannel.sendSync<DownloadingResponse>(request2)
            }
            joinAll(job1, job2)
        }

        assertThat(response1.status, equalTo(Status.OK))
        assertThat(response2.status, equalTo(Status.OK))
    }

}