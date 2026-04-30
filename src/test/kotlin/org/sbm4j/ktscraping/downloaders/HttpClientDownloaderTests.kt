package org.sbm4j.ktscraping.downloaders

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.dowloaders.HttpClientDownloader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull


class HttpClientDownloaderTests: AbstractDownloaderTester<HttpClientDownloader>() {

    override fun buildNode(): HttpClientDownloader {
        val result = HttpClientDownloader("http downloader")
        result.inChannel = inChannel
        return result
    }

    @Test
    fun testDownloadImage1() = testScope.runTest {
        val request = Request(sender, "https://fr.wikipedia.org/static/images/icons/wikipedia.png")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertNotNull(response)
        assertThat(response.status, equalTo(Status.OK))

        val imageBytes = response.contents[AbstractDownloader.PAYLOAD] as ByteArray

        val root = this.javaClass.getResource("/")?.file
        val f = File(root, "downloaded_image.png")
        f.writeBytes(imageBytes)


        f.delete()
    }

    @Test
    fun testDownloadImage2() = TestScope().runTest {
        val request = Request(sender, "https://fr.wikipe/static/images/icons/wikipedia.png")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertThat(response.status, equalTo(Status.FAIL))
    }


    @Test
    fun testDownloadImageSVG() = TestScope().runTest {
        val request = Request(sender, "https://www.iana.org/_img/2022/iana-logo-header.svg")
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertThat(response.status, equalTo(Status.OK))
        println(response.contents["payload"])
    }


}