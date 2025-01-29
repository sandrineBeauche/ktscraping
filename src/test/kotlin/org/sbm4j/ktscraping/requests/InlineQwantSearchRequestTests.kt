package org.sbm4j.ktscraping.requests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.dowloaders.HttpClientDownloader
import kotlin.test.Test

class InlineQwantSearchRequestTests: AbstractDownloaderTester() {

    override fun buildDownloader(sc: CoroutineScope, downloaderName: String): AbstractDownloader {
        return HttpClientDownloader(sc, downloaderName)
    }

    @Test
    fun testURLBuilding(){
        val req = InlineQwantSearchRequest(sender, "Le mariage des lapins", 1)
        assertThat(req.url, equalTo("https://www.qwant.com/?l=fr&t=images&q=Le%20mariage%20des%20lapins"))
    }

    @Test
    fun testDownloading() = TestScope().runTest {
        val req = InlineQwantSearchRequest(sender, "Le mariage des lapins", 1)
        val response = sendRequest(req)
        println(response?.contents?.get("payload"))
    }

}