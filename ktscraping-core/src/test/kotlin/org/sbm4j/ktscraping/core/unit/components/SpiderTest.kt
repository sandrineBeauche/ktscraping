package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.utils.AbstractSpiderTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.SendException
import org.sbm4j.meercat.nodes.logger



class
TestingAbstractSpider(): AbstractSpider("spider") {
    val url = "an url"
    lateinit var resp: DownloadingResponse
    lateinit var ack: ItemAck

    override suspend fun performScraping() {
        val req = Request(this, url)
        resp = sendSync(req) as DownloadingResponse

        val number = resp.contents["body"] as Int
        logger.debug{"${name}: received a response and send an item"}
        val item = IntDataItem(number, this)
        ack = sendSync(item) as ItemAck
    }

}

class SpiderTest: AbstractSpiderTester<TestingAbstractSpider>() {


    override fun buildNode(): TestingAbstractSpider {
        val result = TestingAbstractSpider()
        result.outChannel = outChannel
        return result
    }



    @Test
    fun `request-response then item-itemAck`() = testScope.runTest {
        val cs = stub as ComponentStub
        val contents = mutableMapOf<String, Any>("body" to 1)
        cs.downloadingResponses[node.url] = Pair(ContentType.STRING, contents)

        startAndWait()

        val captured = getReceivedSend()
        assertThat(captured, hasSize(equalTo(4)))
        assertThat(captured[2],
            isA<IntDataItem>(
                has(IntDataItem::data, equalTo(1))
            )
        )
    }


    @Test
    fun testPerformScrapingError() = testScope.runTest {
        val expectedEx = Exception("an exception")
        respondWithError(predicate = {it is AbstractRequest},
            ex = expectedEx)

        startAndWait()

        val captured = getReceivedSend()
        assertThat(captured, hasSize(equalTo(3)))

        val internal = getReceivedInternal()
        assertThat(
            internal[0], isA<ErrorInternal>(
                has(
                    ErrorInternal::errorInfo, isA<ErrorInfo>(
                        has(ErrorInfo::ex, isA<SendException>())
                    )
                )
            )
        )
    }

}