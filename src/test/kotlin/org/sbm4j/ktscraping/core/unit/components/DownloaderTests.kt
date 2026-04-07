package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestingDownloader(): AbstractDownloader("downloader"){
    companion object{
        val URL_OK = "an url"
        val URL_ERROR = "an url with error"
        val URL_EXCEPTION = "an url with exception"
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return when(request.url){
            URL_OK -> request.buildBack()
            URL_ERROR -> {
                val ex = Exception("an error")
                val error = generateErrorInfos(ex)
                request.buildErrorBack(error, Status.ERROR)
            }
            URL_EXCEPTION -> {
                throw Exception("an exception")
            }
            else -> null
        }
    }

    override suspend fun preStart(event: Event): EventJobResult? {
        logger.info { "${name}: inside pre start" }
        return null
    }

    override suspend fun preEnd(event: Event): EventJobResult? {
        logger.info { "${name}: inside pre end" }
        return null
    }
}

class DownloaderTests: AbstractDownloaderTester<TestingDownloader>(){

    override fun buildNode(): TestingDownloader {
        val result = TestingDownloader()
        result.inChannel = inChannel
        return result
    }


    @Test
    fun `request with response`() = testScope.runTest {
        val request = Request(sender, TestingDownloader.URL_OK)
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        val req = response.send
        assertEquals(req.url, TestingDownloader.URL_OK)
    }

    @Test
    fun `request with error`() = testScope.runTest {
        val request = Request(sender, TestingDownloader.URL_ERROR)
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertThat(response.type, equalTo(ContentType.NOTHING ))
        assertThat(response.status, equalTo(Status.ERROR ))
        assertThat(response.errorInfos, hasSize(equalTo(1)))
    }

    @Test
    fun `request with exception`() = testScope.runTest {
        val request = Request(sender, TestingDownloader.URL_EXCEPTION)
        lateinit var response: DownloadingResponse

        withConsumer {
            response = inChannel.sendSync<DownloadingResponse>(request)
        }

        assertThat(response.type, equalTo(ContentType.NOTHING ))
        assertThat(response.status, equalTo(Status.ERROR ))
        assertThat(response.errorInfos, hasSize(equalTo(1)))
    }
}