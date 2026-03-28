package org.sbm4j.ktscraping.core.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import kotlin.test.BeforeTest

abstract class AbstractDownloaderTester: ScrapingTest() {

    lateinit var downloader: AbstractDownloader

    val downloaderName: String = "Downloader"

    abstract fun buildDownloader(downloaderName: String): AbstractDownloader

    @BeforeTest
    fun setUp(){
        buildChannels()

        downloader = buildDownloader(downloaderName)
        downloader.inChannel = inChannel
    }

    suspend fun withDownloader(func: suspend AbstractDownloaderTester.() -> Unit) {
        coroutineScope {
            initChannels(this)

            launch{
                downloader.start(this)?.join()
                doStartEvent()

                func()

                doEndEvent()

                downloader.stop()
                closeChannels()
            }
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