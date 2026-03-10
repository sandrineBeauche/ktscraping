package org.sbm4j.ktscraping.core.unit.components

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.meercat.components.logger
import org.sbm4j.meercat.components.EventJobResult
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloaderTests: AbstractDownloaderTester() {

    private val url: String = "an url"

    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        return object: AbstractDownloader(downloaderName){
            override suspend fun processDataRequest(request: DownloadingRequest): Any? {
                return request.buildBack()
            }

            override suspend fun preStart(event: Event): EventJobResult? {
                logger.info { "${name}: inside pre start" }
                return null
            }


            override suspend fun preEnd(event: Event): EventJobResult? {
                logger.info { "${name}: inside pre end" }
                return null
            }
        }
    }


    @Test
    fun testDownloader() = TestScope().runTest {
        val request = Request(sender, url)
        lateinit var response: DownloadingResponse

        withDownloader {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        val req = response.send
        assertEquals(req.url, url)
    }
}