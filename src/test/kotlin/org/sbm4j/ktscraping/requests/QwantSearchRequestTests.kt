package org.sbm4j.ktscraping.requests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.utils.AbstractPlaywrightRequestDownloadTester
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.middleware.ImageMiddleware
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNotNull

class QwantSearchRequestTests: AbstractPlaywrightRequestDownloadTester(false) {

    @Test
    fun testQwantSearchRequest() = TestScope().runTest {
        withDownloader {
            //repeat(10) {
                val request = QwantSearchRequest(sender, "Le mariage des lapins", 1)
                inChannel.send(request)
                val response = outChannel.receive()

                assertNotNull(response)
                //println(response.contents["payload"])

                val results = response.contents["results"]!! as Map<String, Any>
                val bytes = results["screenshot_0"] as ByteArray

                val root = this.javaClass.getResource("/")?.file
                val f = File(root, "downloaded_image.png")
                f.writeBytes(bytes)
                println(f.absolutePath)

                val stringImage = Base64.getEncoder().encodeToString(bytes)
                println(stringImage)

                //f.delete()
            //}
        }
    }
}

class QwantSearchRequestMiddlewareTests: AbstractSpiderMiddlewareTester(){
    override fun buildMiddleware(sc: CoroutineScope, middlewareName: String): SpiderMiddleware {
        return ImageMiddleware(sc, middlewareName)
    }

    @Test
    fun testImageScraping() = TestScope().runTest {
        val html = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/qwantSearchImage.html")?.readText()

        val request = QwantSearchRequest(sender, "Le mariage des lapins", 1)
        val response = Response(request)
        response.contents["payload"] = html!!

        val expectedImageUrl = "https://s2.qwant.com/thumbr/474x615/f/b/22123eecfae367047c3f39234e8fa2c039823c5b9718962175fc01d4c7ae7a/th.jpg?u=https%3A%2F%2Ftse.mm.bing.net%2Fth%3Fid%3DOIP.9PM0IeSRijljkLU_XEMlHwHaJn%26pid%3DApi&amp;q=0&amp;b=1&amp;p=0&amp;a=0"

        withMiddleware {
            inChannel.send(request)
            followInChannel.receive()

            outChannel.send(response)
            val reqImage = followInChannel.receive()

            println(reqImage)
        }
    }

}