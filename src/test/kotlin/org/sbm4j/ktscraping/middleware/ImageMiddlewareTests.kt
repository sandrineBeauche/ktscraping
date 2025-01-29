package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.requests.GoogleSearchImageRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.Test

class ImageMiddlewareTests: AbstractSpiderMiddlewareTester() {
    override fun buildMiddleware(sc: CoroutineScope, middlewareName: String): SpiderMiddleware {
        val result = ImageMiddleware(sc, middlewareName)
        return result
    }

    @Test
    fun testImageMiddleware1() = TestScope().runTest {
        val html = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/example-domains.html")?.readText()
        val request = Request(sender, "server1")
        request.parameters["cssSelectorImages"] = mapOf("header" to 0)

        val response = Response(request)
        response.contents["payload"] = html!!

        lateinit var resp: Response

        withMiddleware {
            inChannel.send(request)
            val req = followInChannel.receive()

            outChannel.send(response)

            val reqImage = followInChannel.receive()
            logger.debug { "Tester: received the request ${reqImage.name} for the image ${reqImage.url}" }

            val respImage = Response(reqImage)
            val svgImage = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/iana-logo-header.svg")?.readText()
            respImage.contents["payload"] = svgImage!!
            outChannel.send(respImage)
            logger.debug { "Tester: sent the response for the request ${respImage.request.name}" }

            resp = followOutChannel.receive()
        }

        println(resp)
    }


    @Test
    fun testInlineImageMiddleware() = TestScope().runTest {
        val html = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/googleSearchImage.html")?.readText()
        val request = Request(sender, "server1")
        request.parameters["cssSelectorImages"] = mapOf("div#search g-img" to 1)

        val response = Response(request)
        response.contents["payload"] = html!!

        lateinit var resp: Response

        withMiddleware {
            inChannel.send(request)
            val req = followInChannel.receive()

            outChannel.send(response)

            resp = followOutChannel.receive()
        }

        println(resp)
    }

    @Test
    fun testImageFromJson() = TestScope().runTest{
        val json = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/googleSearch.json")?.readText()
        val request = GoogleSearchImageRequest(
            sender, "lectures",
            key = "akbsdlkbjejd",
            searchEngine = "lkjmsljdsd"
            )

        val response = Response(request)
        response.contents["payload"] = json!!

        lateinit var resp: Response

        withMiddleware {
            inChannel.send(request)
            val req = followInChannel.receive()


            outChannel.send(response)
            resp = followOutChannel.receive()
        }

        println(resp)
    }
}