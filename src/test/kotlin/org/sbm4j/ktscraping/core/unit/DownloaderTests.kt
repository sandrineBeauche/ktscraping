package org.sbm4j.ktscraping.core.unit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloaderTests: AbstractDownloaderTester() {

    private val url: String = "an url"

    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        return object: AbstractDownloader(downloaderName){
            override suspend fun processRequest(request: AbstractRequest): Any? {
                return Response(request, Status.OK)
            }
        }
    }


    @Test
    fun testDownloader() = TestScope().runTest{
        val request = Request(sender, url)
        lateinit var response: Response

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive()
        }

        assertEquals(response.request.url, url)
    }
}