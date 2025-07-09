package org.sbm4j.ktscraping.downloaders.playwright

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Status
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlaywrightDownloaderTests: AbstractDownloaderTester() {
    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        val result = PlaywrightDownloader(downloaderName)
        result.headless = true
        return result
    }

    @Test
    fun testSimpleDownload() = TestScope().runTest {
        val request = Request(sender, "https://playwright.dev")
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        assertNotNull(response)
    }

    @Test
    fun testSVGImageDownload() = TestScope().runTest {
        val request = Request(sender, "https://www.iana.org/_img/2022/iana-logo-header.svg")
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        assertNotNull(response)
    }

    @Test
    fun testPNGImageDownload() = TestScope().runTest {
        val request = Request(sender, "https://fr.wikipedia.org/static/images/icons/wikipedia.png")
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        assertNotNull(response)
    }

    @Test
    fun testMultiple() = TestScope().runTest {
        val request = (1..4).map {
            PlaywrightRequest(sender, "https://playwright.dev"){
                waitForTimeout(2000.0)
            }
        }

        lateinit var response1: DownloadingResponse
        lateinit var response2: DownloadingResponse

        withDownloader {
            coroutineScope {
                request.forEach{
                    launch {
                        inChannel.send(it)
                        response1 = outChannel.receive() as DownloadingResponse
                    }
                }
            }
        }

        println("coucou")

        //assertThat(response1.status, equalTo(Status.OK))
        //assertThat(response2.status, equalTo(Status.OK))
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

        withDownloader {
            val job1 =launch {
                inChannel.send(request1)
                response1 = outChannel.receive() as DownloadingResponse
            }
            val job2 = launch {
                inChannel.send(request2)
                response2 = outChannel.receive() as DownloadingResponse
            }
            joinAll(job1, job2)


        }

        assertThat(response1.status, equalTo(Status.OK))
        assertThat(response2.status, equalTo(Status.OK))
    }

}