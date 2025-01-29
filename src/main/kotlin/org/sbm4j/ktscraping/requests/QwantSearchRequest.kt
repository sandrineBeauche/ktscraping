package org.sbm4j.ktscraping.requests

import com.microsoft.playwright.Page
import org.apache.hc.core5.net.URIBuilder
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightRequest

class QwantSearchRequest(
    override val sender: RequestSender,
    searchText: String,
    nbResults: Int
): PlaywrightRequest(sender,"https://www.qwant.com/",
//    mapOf(
//        "l" to "fr",
//        "t" to "images",
//        "q" to searchText
//    ),
    { results ->
        waitForTimeout(1000.0)
        val search = this.locator("input[name='q']")
        search.click();
        search.fill(searchText);
//    search.press("Enter");
//    this.getByTestId("imagesNavItem").waitFor();
        this.getByTestId("imagesNavItem").click();
        this.getByTestId("images").waitFor()
        this.getByTestId("imageResult").first().waitFor()
        repeat(nbResults) {
            val currentLoc = this.getByTestId("imageResult").locator("img").nth(it)
            currentLoc.waitFor()
            waitForTimeout(100.0)
            val bytes = currentLoc.screenshot()
            results["screenshot_${it}"] = bytes
        }

    }) {

    init {
        parameters[PlaywrightDownloader.CONTEXT_NAME] = "qwant"
    }
}


open class AbstractInlineRequest(
    override val sender: RequestSender,
    url: String,
    params: Map<String, String>
): AbstractRequest(sender, url){
    init {
        var builder = URIBuilder(url)
        params.forEach{key, value -> builder = builder.addParameter(key, value)}
        this.url = builder.toString()
    }
}

open class PlaywrightInlineRequest(
    override val sender: RequestSender,
    url: String,
    params: Map<String, String>,
    func: Page.(results: MutableMap<String, Any>) -> Unit
): PlaywrightRequest(sender, url, func){
    init {
        var builder = URIBuilder(url)
        params.forEach{key, value -> builder = builder.addParameter(key, value)}
        this.url = builder.toString()
    }
}

class InlineQwantSearchRequest(
    override val sender: RequestSender,
    val searchText: String,
    nbResults: Int
): AbstractInlineRequest(sender, "https://www.qwant.com/", mapOf(
    "l" to "fr",
    "t" to "images",
    "q" to searchText
)){

    init {
        parameters["cssSelectorImages"] = mapOf("a[data-testId='imageResult']" to nbResults)
    }
}