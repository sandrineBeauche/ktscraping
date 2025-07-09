package org.sbm4j.ktscraping.middleware

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.startsWith
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType

import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.GoogleSearchImageRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test
import kotlin.test.assertNotNull

class ImageMiddlewareTests: AbstractSpiderMiddlewareTester() {
    override fun buildMiddleware(middlewareName: String): SpiderMiddleware {
        val result = ImageMiddleware(middlewareName)
        return result
    }

    @Test
    fun testImageMiddleware1() = TestScope().runTest {
        val html = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/example-domains.html")?.readText()
        val request = Request(sender, "server1")
        request.parameters["cssSelectorImages"] = mapOf("header" to 0)

        val response = DownloadingResponse(request)
        response.contents[AbstractDownloader.PAYLOAD] = html!!

        lateinit var resp: DownloadingResponse

        withMiddleware {
            inChannel.send(request)
            val req = forwardInChannel.receive()

            outChannel.send(response)

            val reqImage = forwardInChannel.receive() as DownloadingRequest
            logger.debug { "Tester: received the request ${reqImage.name} for the image ${reqImage.url}" }

            val respImage = DownloadingResponse(reqImage)
            val svgImage = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/iana-logo-header.svg")?.readText()
            respImage.contents["payload"] = svgImage!!
            outChannel.send(respImage)
            logger.debug { "Tester: sent the response for the request ${respImage.request.name}" }

            resp = forwardOutChannel.receive() as DownloadingResponse
        }

        val contents = resp.contents
        val payload = contents[AbstractDownloader.PAYLOAD] as String
        assertThat(payload, startsWith("<html>"))

        val images = contents[ImageMiddleware.IMAGES_PAYLOAD] as Map<*,*>
        assertThat(images.entries, hasSize(equalTo(1)))

        val img = images["/_img/2022/iana-logo-header.svg"] as ImageDescriptor
        assertThat(img.rawStringData!!, startsWith("<svg"))
    }


    @Test
    fun testInlineImageMiddleware() = TestScope().runTest {
        val html = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/googleSearchImage.html")?.readText()
        val request = Request(sender, "server1")
        request.parameters["cssSelectorImages"] = mapOf("div#search g-img" to 1)

        val response = DownloadingResponse(request)
        response.contents[AbstractDownloader.PAYLOAD] = html!!

        lateinit var resp: DownloadingResponse

        withMiddleware {
            inChannel.send(request)
            val req = forwardInChannel.receive()

            outChannel.send(response)

            resp = forwardOutChannel.receive() as DownloadingResponse
        }

        val contents = resp.contents
        val payload = contents[AbstractDownloader.PAYLOAD] as String
        assertThat(payload, startsWith("<!DOCTYPE html>"))

        val images = contents[ImageMiddleware.IMAGES_PAYLOAD] as Map<*,*>
        assertThat(images.entries, hasSize(equalTo(1)))

        val img = images["La stratégie Ender : Card,Orson Scott, Guillot,Sébastien: Amazon.fr: Livres"] as ImageDescriptor
        assertThat(img.rawStringData!!, startsWith("data:image/"))
    }

    @Test
    fun testImageFromJson() = TestScope().runTest{
        val json = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/googleSearch.json")?.readText()
        val request = GoogleSearchImageRequest(
            sender, "lectures",
            key = "akbsdlkbjejd",
            searchEngine = "lkjmsljdsd"
            )

        val response = DownloadingResponse(request, type = ContentType.JSON)
        response.contents[AbstractDownloader.PAYLOAD] = json!!

        lateinit var resp: DownloadingResponse

        val bytesImage = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/le_mariage_des_lapins.jpg")?.readBytes()!!

        withMiddleware {
            inChannel.send(request)
            val req = forwardInChannel.receive()
            logger.info { "received the followed request, now send response" }

            outChannel.send(response)

            val reqImage = forwardInChannel.receive() as DownloadingRequest
            val respImage = DownloadingResponse(reqImage, type = ContentType.IMAGE)
            respImage.contents[AbstractDownloader.PAYLOAD] = bytesImage
            outChannel.send(respImage)

            resp = forwardOutChannel.receive() as DownloadingResponse
        }

        val contents = resp.contents
        val payload = contents[AbstractDownloader.PAYLOAD] as String
        assertThat(payload, startsWith("{"))

        val images = contents[ImageMiddleware.IMAGES_PAYLOAD] as Map<*,*>
        assertThat(images.entries, hasSize(equalTo(1)))

        val img = images["https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_op-v8BH8ofh8ZWs9Dhj15DXdr6axwcZEpMYmCB1_2B_O3HXSl7jzVQ&s"] as ImageDescriptor
        assertNotNull(img.rawBytesData)
    }
}