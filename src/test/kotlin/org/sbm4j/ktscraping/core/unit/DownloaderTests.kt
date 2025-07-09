package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloaderTests: AbstractDownloaderTester() {

    private val url: String = "an url"

    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        return object: AbstractDownloader(downloaderName){
            override suspend fun processDataRequest(request: DownloadingRequest): Any? {
                return DownloadingResponse(request)
            }

            override suspend fun preStart(event: Event): Job? {
                logger.info { "${name}: inside pre start" }
                return super.preStart(event)
            }

            override suspend fun postStart(event: EventResponse) {
                logger.info { "${name}: inside post start" }
                super.postStart(event)
            }

            override suspend fun preEnd(event: Event): Job? {
                logger.info { "${name}: inside pre end" }
                return super.preEnd(event)
            }

            override suspend fun postEnd(event: EventResponse) {
                logger.info { "${name}: inside post end" }
                super.postEnd(event)
            }
        }
    }


    @Test
    fun testDownloader() = TestScope().runTest{
        val request = Request(sender, url)
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        val req = response.request
        assertEquals(req.url, url)
    }
}