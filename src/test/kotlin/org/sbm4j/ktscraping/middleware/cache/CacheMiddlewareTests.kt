package org.sbm4j.ktscraping.middleware.cache

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.utils.AbstractMiddlewareTester
import org.sbm4j.ktscraping.middleware.CacheAvailability
import org.sbm4j.ktscraping.middleware.CacheEntry
import org.sbm4j.ktscraping.middleware.CacheMiddleware
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import java.io.File
import kotlin.test.Test


class CacheMiddlewareUnitTests{

    fun performTest(entries: List<CacheEntry>): List<String>{
        val middleware1 = CacheMiddleware("middleware1")

        val rootCache = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware")!!
        middleware1.root = File(rootCache.file)

        middleware1.cacheMap.putAll(entries.associateBy { it.key })

        middleware1.saveCache("cache1.json")

        val middleware2 = CacheMiddleware("midddleware2")
        middleware2.root = middleware1.root
        middleware2.availability = CacheAvailability.DAY
        middleware2.loadCache("cache1.json")

        val f = File(middleware1.root, "cache1.json")
        if(f.exists()){
            f.delete()
        }

        return middleware2.cacheMap.keys.toList()
    }

    @Test
    fun testSaveLoadCache1() = TestScope().runTest{
        val entry1 = CacheEntry(
            "key1",
            CacheAvailability.DAY,
            payload = "blabla",
            frames = mapOf(
                "frame1" to "blablaFrame1",
                "frame2" to "blablaFrame2"
            )
        )

        val entry2 = CacheEntry(
            "key2",
            CacheAvailability.HOUR,
            payload = "toto",
            frames = mapOf(
                "frame1" to "totoFrame1",
                "frame2" to "totoFrame2"
            )
        )

        val entries = listOf(entry1, entry2)
        val keys = performTest(entries)

        assertThat(keys, isA<List<String>>(
            allOf(
                hasSize(equalTo(2)),
                hasElement("key1"),
                hasElement("key2")
            )

        ))
    }

    @Test
    fun testSaveLoadCache2() = TestScope().runTest{
        val entry1 = CacheEntry(
            "key1",
            CacheAvailability.DAY,
            payload = "blabla"
        )

        val entry2 = CacheEntry(
            "key2",
            CacheAvailability.HOUR,
            payload = "toto",
            timestamp = 0
        )

        val entries = listOf(entry1, entry2)
        val keys = performTest(entries)

        assertThat(keys, isA<List<String>>(
            allOf(
                hasSize(equalTo(1)),
                hasElement("key1"),
            )
        ))
    }
}

class CacheMiddlewareTests: AbstractMiddlewareTester() {
    override fun buildMiddleware(middlewareName: String): AbstractMiddleware {
        val rootCache = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware")!!

        val result = CacheMiddleware(middlewareName)
        result.availability = CacheAvailability.HOUR
        result.root = File(rootCache.file, "cache")
        if(!result.root.exists()){
            result.root.mkdir()
        }

        return result
    }

    @Test
    fun testCacheMiddlewareEmpty() = TestScope().runTest {
        val request = Request(sender, "http://www.exemple.com")
        val response = DownloadingResponse(request)

        val respFilename = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/example-domains.html")!!.file
        response.contents[AbstractDownloader.PAYLOAD] = File(respFilename).readText()
        response.contents[AbstractDownloader.CONTENT_TYPE] = ContentType.HTML

        lateinit var req: Request
        lateinit var resp: DownloadingResponse
        lateinit var resp2: DownloadingResponse

        withMiddleware {
            inChannel.send(request)
            req = outChannel.receive() as Request

            outChannel.send(response)
            resp = forwardOutChannel.receive() as DownloadingResponse

            inChannel.send(request)
            resp2 = forwardOutChannel.receive() as DownloadingResponse
        }

        val contentType = resp2.type
        assertThat(contentType, equalTo(ContentType.HTML))
    }

    @Test
    fun testCacheMiddlewareLoadCache() = TestScope().runTest {
        val cacheDir = this.javaClass.getResource("/org.sbm4j.ktscraping/middleware/cache1")!!.file
        (middleware as CacheMiddleware).root = File(cacheDir)

        val request = Request(sender, "http://www.exemple.com")
        lateinit var resp: DownloadingResponse

        withMiddleware {
            inChannel.send(request)
            resp = forwardOutChannel.receive() as DownloadingResponse
        }

        val contentType = resp.type
        assertThat(contentType, equalTo(ContentType.HTML))
    }
}