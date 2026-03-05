package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.utils.isDownloadingRequestWith
import org.sbm4j.ktscraping.core.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Response


class MiddlewareClassTest(name: String): AbstractMiddleware(name){
    override suspend fun processResponse(response: Response) {
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
        val startEvent = StartEvent(sender)
        val back = channelManager.downloaderChannel.sendSync<EventBack>(startEvent)
    }

    suspend fun sendEndEvent(){
        val endEvent = EndEvent(sender)
        val back = channelManager.downloaderChannel.sendSync<EventBack>(endEvent)
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
        val response = channelManager.downloaderChannel.sendSync<DownloadingResponse>(request1)

        logger.debug { "Received the response: $response" }

        sendEndEvent()
        c.stop()
        channelManager.closeChannels()

        val respReq = response.send
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
                { req: AbstractRequest ->
                    if (req is DownloadingRequest && req.url == url1) receivers[0]
                    else receivers[1]
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

        channelManager.downloaderChannel.send(request1)
        channelManager.downloaderChannel.send(request2)

        response1 = channelManager.downloaderChannel.receiveBack<DownloadingResponse>()
        response2 = channelManager.downloaderChannel.receiveBack<DownloadingResponse>()

        logger.debug { "Received the responses" }

        sendEndEvent()
        c.stop()
        channelManager.closeChannels()

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
                { req: AbstractRequest ->
                    if (req is DownloadingRequest && req.url == url1) receivers[0]
                    else receivers[1]
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

        channelManager.downloaderChannel.send(request1)
        channelManager.downloaderChannel.send(request2)

        val response1: DownloadingResponse = channelManager.downloaderChannel.receiveBack<DownloadingResponse>()
        val response2: DownloadingResponse = channelManager.downloaderChannel.receiveBack<DownloadingResponse>()

        logger.debug { "Received the responses" }

        sendEndEvent()
        c.stop()
        channelManager.closeChannels()

        assertThat(response1, isDownloadingResponseWith(url1,
            mutableMapOf("downloader" to "Downloader1")))

        assertThat(response2, isDownloadingResponseWith(url2,
            mutableMapOf("downloader" to "Downloader2")))

    }

}