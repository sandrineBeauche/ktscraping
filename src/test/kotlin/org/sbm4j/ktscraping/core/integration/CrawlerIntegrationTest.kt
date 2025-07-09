package org.sbm4j.ktscraping.core.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.AbstractSpider
import org.sbm4j.ktscraping.core.defaultDIModule
import org.sbm4j.ktscraping.core.dsl.crawler
import org.sbm4j.ktscraping.core.dsl.downloaderBranch
import org.sbm4j.ktscraping.core.dsl.pipelineBranch
import org.sbm4j.ktscraping.core.dsl.spiderBranch
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Status
import kotlin.test.Test

data class IntegrationTestItem(val value: String, ): Item() {
    override fun clone(): Item {
        return this.copy()
    }
}

class IntegrationTestSpider(
    name: String
): AbstractSpider(name){
    override suspend fun performScraping(subScope: CoroutineScope) {
        val req1 = Request(this, "request1")
        val resp1 = sendSync(req1) as DownloadingResponse
        val value = resp1.contents["prop1"] as String
        val result = IntegrationTestItem(value)
        this.itemsOut.send(result)
    }
}


class IntegrationTestDownloader(
    name: String = "TestDownloader"
): AbstractDownloader(name){
    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        val response = DownloadingResponse(request)
        response.contents["prop1"] = "value1"
        return response
    }

}

class IntegrationTestExporter(
    name: String
): AbstractExporter(name){
    override fun exportItem(item: Item) {
        println(item)
    }

}

class CrawlerIntegrationTest {

    @Test
    fun integrationTest1() = TestScope().runTest{
        val crawler = crawler("TestCrawler", {name -> defaultDIModule(name) }){
            spiderBranch {
                spider<IntegrationTestSpider>("spider")
            }
            downloaderBranch {
                downloader<IntegrationTestDownloader>("downloader")
            }
            pipelineBranch {
                exporter<IntegrationTestExporter>("exporter")
            }
        }

        crawler.start(this)
        val result = crawler.waitFinished()
        crawler.stop()

        println(result)
    }
}