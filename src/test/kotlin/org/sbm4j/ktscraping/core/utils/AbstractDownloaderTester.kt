package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.dowloaders.PlaywrightDownloader
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.BeforeTest

abstract class AbstractDownloaderTester: ScrapingTest<AbstractRequest, Response>() {

    lateinit var downloader: AbstractDownloader

    val sender: RequestSender = mockk<RequestSender>()

    val downloaderName: String = "Downloader"

    abstract fun buildDownloader(sc: CoroutineScope, downloaderName: String): AbstractDownloader

    @BeforeTest
    fun setUp(){
        initChannels()
        clearAllMocks()

        val sc = mockk<CoroutineScope>()

        downloader = spyk(buildDownloader(sc, downloaderName))

        every { downloader.requestIn } returns inChannel
        every { downloader.responseOut } returns outChannel
    }

    suspend fun withDownloader(func: suspend AbstractDownloaderTester.() -> Unit){
        coroutineScope {
            every { downloader.scope } returns this

            downloader.start()

            func()

            closeChannels()
            downloader.stop()
        }
    }

    protected suspend fun sendRequest(request: AbstractRequest): Response?{
        var response: Response? = null
        withDownloader {
            inChannel.send(request)
            response = outChannel.receive()
        }
        return response
    }
}

abstract class AbstractPlaywrightRequestDownloadTester(val headless: Boolean): AbstractDownloaderTester(){

    override fun buildDownloader(sc: CoroutineScope, downloaderName: String): AbstractDownloader {
        val play = PlaywrightDownloader(sc, downloaderName)
        play.headless = headless
        return play
    }
}