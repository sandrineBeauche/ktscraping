package org.sbm4j.ktscraping.core.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import org.sbm4j.ktscraping.requests.*
import java.util.*
import kotlin.test.Test

data class IntegrationTestItem(val value: String, override val id: UUID = UUID.randomUUID()): Item {
    override fun clone(): Item {
        return this.copy()
    }
}

class IntegrationTestSpider(
    scope: CoroutineScope,
    name: String
): AbstractSpider(scope, name){
    override suspend fun performScraping() {
        val req1 = Request(this, "request1")
        val resp1 = sendSync(req1)
        val value = resp1.contents["prop1"] as String
        val result = IntegrationTestItem(value)
        this.itemsOut.send(result)
    }
}


class IntegrationTestDownloader(
    scope: CoroutineScope,
    name: String = "TestDownloader"
): AbstractDownloader(scope, name){
    override suspend fun processRequest(request: AbstractRequest): Any? {
        val response = Response(request, Status.OK)
        response.contents["prop1"] = "value1"
        return response
    }

}

class IntegrationTestExporter(
    scope: CoroutineScope,
    name: String
): AbstractExporter(scope, name){
    override fun exportItem(item: Item) {
        println(item)
    }

}

class CrawlerIntegrationTest {

    @Test
    fun integrationTest1() = TestScope().runTest{
        val crawler = crawler(this, "TestCrawler", {scope, name -> defaultDIModule(scope, name) }){
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

        crawler.start()
        val result = crawler.waitFinished()
        crawler.stop()

        println(result)
    }
}