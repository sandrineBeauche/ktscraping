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
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse


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

    @Test
    fun testBuildCrawlerWithDownloaderBranch() = scope.runTest{
        val url = "une url"
        lateinit var response: DownloadingResponse

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                downloaderBranch {
                    middleware<MiddlewareClassTest>()
                    downloader<DownloaderClassTest>(name = "Downloader1")
                }
            }

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val request1 = Request(sender, url)
                channelFactory.downloaderRequestChannel.send(request1)
                response = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

                logger.debug { "Received the response" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        val respReq = response.request as DownloadingRequest
        assertThat(respReq.url, equalTo(url))
    }


    @Test
    fun testBuildCrawlerWithDownloaderDispatcher() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"

        lateinit var response1: DownloadingResponse
        lateinit var response2: DownloadingResponse

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                downloaderDispatcher("dispatcher1",
                    {req: DownloadingRequest ->
                        if(req.url == url1) senders[0]
                        else senders[1]
                    })
                {
                    downloader<DownloaderClassTest>(name = "Downloader1")
                    downloader<DownloaderClassTest>(name = "Downloader2")
                }
            }

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val request1 = Request(sender, url1)
                val request2 = Request(sender, url2)

                channelFactory.downloaderRequestChannel.send(request1)
                channelFactory.downloaderRequestChannel.send(request2)

                response1 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse
                response2 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

                logger.debug { "Received the responses" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(response1, allOf(
            has(DownloadingResponse::request, isA(has(DownloadingRequest::url, equalTo(url1)))),
            has(DownloadingResponse::contents, equalTo(mutableMapOf("downloader" to "Downloader1")))
        ))

        assertThat(response2, allOf(
            has(DownloadingResponse::request, isA(has(DownloadingRequest::url, equalTo(url2)))),
            has(DownloadingResponse::contents, equalTo(mutableMapOf("downloader" to "Downloader2")))
        ))
    }

    @Test
    fun testBuildCrawlerWithDownloaderDispatcherAndBranch() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"

        lateinit var response1: DownloadingResponse
        lateinit var response2: DownloadingResponse

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                downloaderDispatcher("dispatcher1",
                    {req: DownloadingRequest ->
                        if(req.url == url1) senders[0]
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

            launch {
                c.start(this)
            }
            launch {
                logger.debug { "interacting with crawler" }

                val request1 = Request(sender, url1)
                val request2 = Request(sender, url2)

                channelFactory.downloaderRequestChannel.send(request1)
                channelFactory.downloaderRequestChannel.send(request2)

                response1 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse
                response2 = channelFactory.downloaderResponseChannel.receive() as DownloadingResponse

                logger.debug { "Received the responses" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(response1, allOf(
            has(DownloadingResponse::request, isA(has(DownloadingRequest::url, equalTo(url1)))),
            has(DownloadingResponse::contents, equalTo(mutableMapOf("downloader" to "Downloader1")))
        ))

        assertThat(response2, allOf(
            has(DownloadingResponse::request, isA(has(DownloadingRequest::url, equalTo(url2)))),
            has(DownloadingResponse::contents, equalTo(mutableMapOf("downloader" to "Downloader2")))
        ))
    }

}