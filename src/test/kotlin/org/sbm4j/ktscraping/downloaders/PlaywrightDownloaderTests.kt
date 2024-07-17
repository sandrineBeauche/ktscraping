package org.sbm4j.ktscraping.downloaders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.dowloaders.PlaywrightDownloader
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlaywrightDownloaderTests: AbstractDownloaderTester() {
    override fun buildDownloader(sc: CoroutineScope, downloaderName: String): AbstractDownloader {
        return PlaywrightDownloader(sc, downloaderName)
    }

    @Test
    fun testSimpleDownload() = TestScope().runTest {
        val request = Request(sender, "https://playwright.dev")
        lateinit var response: Response

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive()
        }

        assertNotNull(response)
    }

}