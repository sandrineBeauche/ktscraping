package org.sbm4j.ktscraping.example

import it.skrape.core.htmlDocument
import it.skrape.selects.html5.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.core.dsl.crawler
import org.sbm4j.ktscraping.core.dsl.downloaderBranch
import org.sbm4j.ktscraping.core.dsl.pipelineBranch
import org.sbm4j.ktscraping.core.dsl.spiderBranch
import org.sbm4j.ktscraping.dowloaders.HttpClientDownloader
import org.sbm4j.ktscraping.exporters.JSONExporter
import org.sbm4j.ktscraping.requests.Data
import org.sbm4j.ktscraping.requests.Request


@Serializable
data class BoardgameEvent(
    val title: String,
    val url: String,
): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}

class FirstExampleSpider(name: String
) : AbstractSpider(name) {
    override suspend fun performScraping(subScope: CoroutineScope) {
        task("FetchData", taskMessage = "Fetch event data", slotMode = SlotMode.PROGRESS_BAR_UNDEFINED){ task ->
            //sends the initial request and get the response
            val request = Request(this, "http://www.meeple-breton.fr/2025/01/tous-les-festivals-de-2025.html")
            val response = sendSync(request, subScope)
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


@OptIn(InternalSerializationApi::class)
fun buildCrawler(channelResult: Channel<String>): Crawler{
    return crawler("Crawler", {name -> defaultDIModule(name) }){
        spiderBranch {
            spider<FirstExampleSpider>("FirstExampleSpider")
        }
        downloaderBranch {
            downloader<HttpClientDownloader>()
        }
        pipelineBranch {
            exporter<JSONExporter<BoardgameEvent>>{
                out = channelResult
                serializer = BoardgameEvent::class.serializer()
            }
        }
    }
}

suspend fun executeCrawler(crawler: Crawler, channelResult: Channel<String>){
    lateinit var result: CrawlerResult
    coroutineScope {
        launch {
            crawler.start(this@launch)
            val json = channelResult.receive()
            println(json)
        }
        launch{
            result = crawler.waitFinished()
            crawler.stop()
        }
    }
    println(result)
}

fun main(args: Array<String>) = runBlocking{
    val channelResult: Channel<String> = Channel(Channel.RENDEZVOUS)
    val crawler = buildCrawler(channelResult)
    executeCrawler(crawler, channelResult)
}