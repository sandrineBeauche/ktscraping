package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
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
        initChannels()

        downloader = buildDownloader(downloaderName)
        downloader.inChannel = inChannel
    }

    suspend fun withDownloader(func: suspend AbstractDownloaderTester.() -> Unit) {
        coroutineScope {
            inChannel.init()

            launch {
                downloader.start(this)
            }
            launch{
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