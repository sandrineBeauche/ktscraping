package org.sbm4j.ktscraping.downloaders

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.dowloaders.HttpClientDownloader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull


class HttpClientDownloaderTests: AbstractDownloaderTester() {

    override fun buildDownloader(downloaderName: String): AbstractDownloader {
        return HttpClientDownloader(downloaderName)
    }

    @Test
    fun testDownloadImage1() = TestScope().runTest {
        val request = Request(sender, "https://fr.wikipedia.org/static/images/icons/wikipedia.png")
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
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

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        assertThat(response.status, equalTo(Status.NOT_FOUND))
    }


    @Test
    fun testDownloadImageSVG() = TestScope().runTest {
        val request = Request(sender, "https://www.iana.org/_img/2022/iana-logo-header.svg")
        lateinit var response: DownloadingResponse

        withDownloader {
            inChannel.send(request)
            response = outChannel.receive() as DownloadingResponse
        }

        assertThat(response.status, equalTo(Status.OK))
        println(response.contents["payload"])
    }


}