package org.sbm4j.ktscraping.downloaders.playwright

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.utils.hasEntry
import org.sbm4j.ktscraping.utils.hasPayload
import org.sbm4j.meercat.data.Status
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlaywrightDownloaderTests: AbstractDownloaderTester<PlaywrightDownloader>() {

    override fun buildNode(): PlaywrightDownloader {
        val result = PlaywrightDownloader("playwright downloader")
        result.headless = true
        result.inChannel = inChannel
        return result
    }

    @Test
    fun testSimpleDownload() = TestScope().runTest {
        val request = Request(sender, "https://playwright.dev")

        withConsumer {
            val response = inChannel.sendSync<DownloadingResponse>(request)
            assertThat(response, hasPayload(
                startsWith("<!DOCTYPE html><html")
            ))
        }


    }

    @Test
    fun testSVGImageDownload() = TestScope().runTest {
        val request = Request(sender, "https://www.iana.org/_img/2022/iana-logo-header.svg")

        withConsumer {
            val response = inChannel.sendSync<DownloadingResponse>(request)
            assertThat(response, hasPayload(
                startsWith("<!DOCTYPE svg PUBLIC")
            ))
        }
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
            waitForTimeout(500.0)
        }

        val request2 = PlaywrightRequest(sender, "https://playwright.dev"){
            waitForTimeout(500.0)
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