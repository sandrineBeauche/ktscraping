package org.sbm4j.ktscraping.example

import it.skrape.core.htmlDocument
import it.skrape.selects.html5.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.dsl.crawler
import org.sbm4j.ktscraping.core.dsl.downloaderBranch
import org.sbm4j.ktscraping.core.dsl.pipelineBranch
import org.sbm4j.ktscraping.core.dsl.spiderBranch
import org.sbm4j.ktscraping.dowloaders.HttpClientDownloader
import org.sbm4j.ktscraping.exporters.StdOutExporter
import org.sbm4j.ktscraping.pipeline.JSONPipeline
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse


@Serializable
data class BoardgameEvent(
    val title: String,
    val url: String,
): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}

class FirstExampleSpider(
    name: String
) : AbstractSpider(name) {
    override suspend fun performScraping(subScope: CoroutineScope) {
        task("FetchData", taskMessage = "Fetch event data", slotMode = SlotMode.PROGRESS_BAR_UNDEFINED){ task ->
            //sends the initial request and get the response
            val request = Request(this, "http://www.meeple-breton.fr/2025/01/tous-les-festivals-de-2025.html")
            val response = sendSync(request, subScope) as DownloadingResponse
            val html = response.contents["payload"] as String

            //extracts data from the response
            val results = htmlDocument(html) {
                div("#main") {
                    val links = findFirst("div.entry-content"){ eachLink }
                    links.map { (title, url) -> BoardgameEvent(title, url) }
                }
            }

            //sends data to the exporter
            results.forEach { task.sendData(it, "site") }
        }
    }
}


fun buildCrawler(): Crawler{
    return crawler("Crawler", {name -> defaultDIModule(name) }){
        spiderBranch {
            spider<FirstExampleSpider>("FirstExampleSpider")
        }
        downloaderBranch {
            downloader<HttpClientDownloader>()
        }
        pipelineBranch {
            pipeline<JSONPipeline>{ accumulate = true }
            exporter<StdOutExporter>()
        }
    }
}

suspend fun executeCrawler(crawler: Crawler){
    lateinit var result: CrawlerResult
    coroutineScope {
        launch {
            crawler.start(this@launch)
        }
        launch{
            result = crawler.waitFinished()
            crawler.stop()
        }
    }
    println(result)
}

fun main(args: Array<String>) = runBlocking{
    val crawler = buildCrawler()
    executeCrawler(crawler)
}