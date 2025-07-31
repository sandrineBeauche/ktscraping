package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.isDownloadingRequestWith
import org.sbm4j.ktscraping.core.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse


class MiddlewareClassTest(name: String): AbstractMiddleware(name){
    override suspend fun processResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        return true
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }
}

class DownloaderClassTest(name: String) : AbstractDownloader(name){
    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        val resp = DownloadingResponse(request)
        resp.contents["downloader"] = name
        return resp
    }

}

class DownloaderBranchTest: CrawlerTest() {

    suspend fun sendStartEvent(){
        val startReq = StartRequest(sender)
        channelFactory.downloaderRequestChannel.send(startReq)
        val startResp = channelFactory.downloaderResponseChannel.receive() as EventResponse
    }

    suspend fun sendEndEvent(){
        val endReq = EndRequest(sender)
        channelFactory.downloaderRequestChannel.send(endReq)
        val endResp = channelFactory.downloaderResponseChannel.receive() as EventResponse
    }

    @Test
    fun testBuildCrawlerWithDownloaderBranch() = scope.runTest {
        val url = "une url"

        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderBranch {
                middleware<MiddlewareClassTest>()
                downloader<DownloaderClassTest>(name = "Downloader1")
            }
        }


        c.start(this)
        logger.debug { "interacting with crawler" }

        sendStartEvent()

        val request1 = Request(sender, url)
        channelFactory.downloaderRequestChannel.send(request1)
        val response: DownloadingResponse = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

        logger.debug { "Received the response: $response" }

        sendEndEvent()
        c.stop()
        channelFactory.closeChannels()


        val respReq = response.request
        assertThat(respReq, isDownloadingRequestWith(url))
    }


    @Test
    fun testBuildCrawlerWithDownloaderDispatcher() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"

        lateinit var response1: DownloadingResponse
        lateinit var response2: DownloadingResponse


        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderDispatcher(
                "dispatcher1",
                { req: DownloadingRequest ->
                    if (req.url == url1) senders[0]
                    else senders[1]
                })
            {
                downloader<DownloaderClassTest>(name = "Downloader1")
                downloader<DownloaderClassTest>(name = "Downloader2")
            }
        }


        c.start(this)

        logger.debug { "interacting with crawler" }
        sendStartEvent()

        val request1 = Request(sender, url1)
        val request2 = Request(sender, url2)

        channelFactory.downloaderRequestChannel.send(request1)
        channelFactory.downloaderRequestChannel.send(request2)

        response1 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse
        response2 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

        logger.debug { "Received the responses" }

        sendEndEvent()
        c.stop()
        channelFactory.closeChannels()

        assertThat(response1, isDownloadingResponseWith(url1,
            mutableMapOf("downloader" to "Downloader1")))

        assertThat(response2, isDownloadingResponseWith(url2,
            mutableMapOf("downloader" to "Downloader2")))

    }

    @Test
    fun testBuildCrawlerWithDownloaderDispatcherAndBranch() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"


        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderDispatcher(
                "dispatcher1",
                { req: DownloadingRequest ->
                    if (req.url == url1) senders[0]
                    else senders[1]
                })
            {
                downloaderBranch {
                    middleware<MiddlewareClassTest>(name = "Middleware1")
                    downloader<DownloaderClassTest>(name = "Downloader1")
                }
                downloaderBranch {
                    middleware<MiddlewareClassTest>(name = "Middleware2")
                    downloader<DownloaderClassTest>(name = "Downloader2")
                }
            }
        }


        c.start(this)

        logger.debug { "interacting with crawler" }
        sendStartEvent()

        val request1 = Request(sender, url1)
        val request2 = Request(sender, url2)

        channelFactory.downloaderRequestChannel.send(request1)
        channelFactory.downloaderRequestChannel.send(request2)

        val response1: DownloadingResponse = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse
        val response2: DownloadingResponse = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

        logger.debug { "Received the responses" }

        sendEndEvent()
        c.stop()
        channelFactory.closeChannels()

        assertThat(response1, isDownloadingResponseWith(url1,
            mutableMapOf("downloader" to "Downloader1")))

        assertThat(response2, isDownloadingResponseWith(url2,
            mutableMapOf("downloader" to "Downloader2")))

    }

}