package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response


class MiddlewareClassTest(name: String): AbstractMiddleware(name){
    override suspend fun processResponse(response: Response): Boolean {
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        return true
    }
}

class DownloaderClassTest(name: String) : AbstractDownloader(name){
    override suspend fun processRequest(request: AbstractRequest): Any? {
        val resp = Response(request)
        resp.contents["downloader"] = name
        return resp
    }

}

class DownloaderBranchTest: CrawlerTest() {

    @Test
    fun testBuildCrawlerWithDownloaderBranch() = scope.runTest{
        val url = "une url"
        lateinit var response: Response

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
                response = channelFactory.downloaderResponseChannel.receive()

                logger.debug { "Received the response" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(response.request.url, equalTo(url))
    }


    @Test
    fun testBuildCrawlerWithDownloaderDispatcher() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"

        lateinit var response1: Response
        lateinit var response2: Response

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                downloaderDispatcher("dispatcher1",
                    {req: AbstractRequest ->
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

                response1 = channelFactory.downloaderResponseChannel.receive()
                response2 = channelFactory.downloaderResponseChannel.receive()

                logger.debug { "Received the responses" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(response1, allOf(
            has(Response::request, has(AbstractRequest::url, equalTo(url1))),
            has(Response::contents, equalTo(mutableMapOf("downloader" to "Downloader1")))
        ))

        assertThat(response2, allOf(
            has(Response::request, has(AbstractRequest::url, equalTo(url2))),
            has(Response::contents, equalTo(mutableMapOf("downloader" to "Downloader2")))
        ))
    }

    @Test
    fun testBuildCrawlerWithDownloaderDispatcherAndBranch() = scope.runTest{
        val url1 = "url1"
        val url2 = "url2"

        lateinit var response1: Response
        lateinit var response2: Response

        coroutineScope {
            val c = crawler("MainCrawler", ::testDIModule){
                downloaderDispatcher("dispatcher1",
                    {req: AbstractRequest ->
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

                response1 = channelFactory.downloaderResponseChannel.receive()
                response2 = channelFactory.downloaderResponseChannel.receive()

                logger.debug { "Received the responses" }

                c.stop()
                channelFactory.closeChannels()
            }
        }

        assertThat(response1, allOf(
            has(Response::request, has(AbstractRequest::url, equalTo(url1))),
            has(Response::contents, equalTo(mutableMapOf("downloader" to "Downloader1")))
        ))

        assertThat(response2, allOf(
            has(Response::request, has(AbstractRequest::url, equalTo(url2))),
            has(Response::contents, equalTo(mutableMapOf("downloader" to "Downloader2")))
        ))
    }

}