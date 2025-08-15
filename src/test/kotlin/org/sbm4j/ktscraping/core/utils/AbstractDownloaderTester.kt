package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.EventResponse
import org.sbm4j.ktscraping.data.response.Response
import kotlin.test.BeforeTest

abstract class AbstractDownloaderTester: ScrapingTest() {

    lateinit var downloader: AbstractDownloader

    val sender: Controllable = mockk<Controllable>()

    val downloaderName: String = "Downloader"

    abstract fun buildDownloader(downloaderName: String): AbstractDownloader

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        downloader = spyk(buildDownloader(downloaderName))

        every { downloader.inChannel } returns inChannel
    }

    suspend fun withDownloader(func: suspend AbstractDownloaderTester.() -> Unit){
        coroutineScope {
            downloader.start(this@coroutineScope)
            inChannel.init(this@coroutineScope)

            val startEventReq = StartRequest(sender)
            inChannel.sendSync<EventResponse>(startEventReq)

            func()

            val endEventReq = EndRequest(sender)
            inChannel.sendSync<EventResponse>(endEventReq)
            
            closeChannels()
            downloader.stop()
        }
    }

    protected suspend fun sendRequest(request: AbstractRequest): DownloadingResponse?{
        var response: DownloadingResponse? = null
        withDownloader {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }
        return response
    }
}

abstract class AbstractPlaywrightRequestDownloadTester(val headless: Boolean): AbstractDownloaderTester(){

    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        val play = PlaywrightDownloader(downloaderName)
        play.headless = headless
        return play
    }
}