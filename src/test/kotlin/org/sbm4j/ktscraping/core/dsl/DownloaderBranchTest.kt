package org.sbm4j.ktscraping.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.utils.isDownloadingRequestWith
import org.sbm4j.ktscraping.core.utils.isDownloadingResponseWith
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.logger


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

    suspend fun sendRequests(urls: List<String>): List<DownloadingResponse> = coroutineScope{
        val jobs: List<Deferred<DownloadingResponse>> = urls.map { url ->
            async {
                val request = Request(sender, url)
                val result = crawlerChannelManager.downloaderChannel.sendSync<DownloadingResponse>(request)
                result
            }
        }
        return@coroutineScope jobs.awaitAll()
    }

    @Test
    fun `branch with middleware and downloader`() = scope.runTest {
        val url = "une url"

        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderBranch {
                middleware<MiddlewareClassTest>()
                downloader<DownloaderClassTest>(name = "Downloader1")
            }
        }


        c.start(this, "crawler-root")?.join()
        logger.debug { "interacting with crawler" }

        sendStartEvent(crawlerChannelManager.downloaderChannel)

        val request1 = Request(sender, url)
        val response = crawlerChannelManager.downloaderChannel.sendSync<DownloadingResponse>(request1)

        logger.debug { "Received the response: $response" }

        sendEndEvent(crawlerChannelManager.downloaderChannel)
        c.stop()

        val respReq = response.send
        assertThat(respReq, isDownloadingRequestWith(url))
    }


    @Test
    fun `branch with dispatcher`() = scope.runTest{
        val urls = listOf("url1", "url2")


        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderDispatcher(
                "dispatcher1",
                { req: AbstractRequest ->
                    if (req is DownloadingRequest && req.url == urls[0]) channelOuts[0]
                    else channelOuts[1]
                })
            {
                downloader<DownloaderClassTest>(name = "Downloader1")
                downloader<DownloaderClassTest>(name = "Downloader2")
            }
        }


        c.start(this, "crawler-root")?.join()

        logger.debug { "interacting with crawler" }
        sendStartEvent(crawlerChannelManager.downloaderChannel)

        val responses = sendRequests(urls)

        logger.debug { "Received the responses: $responses" }

        sendEndEvent(crawlerChannelManager.downloaderChannel)
        c.stop()

        assertThat(responses[0], isDownloadingResponseWith(urls[0],
            mutableMapOf("downloader" to "Downloader1")))

        assertThat(responses[1], isDownloadingResponseWith(urls[1],
            mutableMapOf("downloader" to "Downloader2")))

    }

    @Test
    fun `branch with dispatcher and middlewares`() = scope.runTest{
        val urls = listOf("url1", "url2")

        val c = crawler("MainCrawler", ::testDIModule) {
            downloaderDispatcher(
                "dispatcher1",
                { req: AbstractRequest ->
                    if (req is DownloadingRequest && req.url == urls[0]) channelOuts[0]
                    else channelOuts[1]
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

        c.start(this, "crawler-root")?.join()

        logger.debug { "interacting with crawler" }
        sendStartEvent(crawlerChannelManager.downloaderChannel)

        val responses = sendRequests(urls)

        logger.debug { "Received the responses" }

        sendEndEvent(crawlerChannelManager.downloaderChannel)
        c.stop()
        crawlerChannelManager.closeChannels()

        assertThat(responses[0], isDownloadingResponseWith(urls[0],
            mutableMapOf("downloader" to "Downloader1")))

        assertThat(responses[1], isDownloadingResponseWith(urls[1],
            mutableMapOf("downloader" to "Downloader2")))

    }

}