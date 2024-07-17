package org.sbm4j.ktscraping.dowloaders

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status

class PlaywrightDownloader(scope: CoroutineScope, name: String) : AbstractDownloader(scope, name) {

    lateinit var browser: Browser

    override suspend fun start() {
        super.start()
        val playwright = Playwright.create()
        browser = playwright.chromium().launch()
    }

    override suspend fun stop() {
        super.stop()
        browser.close()
    }

    override fun processRequest(request: Request): Any? {
        val context = getContext(request)
        val page = context.newPage()
        page.navigate(request.url)

        if(request.parameters.containsKey("playwright")){
            val func = request.parameters["playwright"] as Page.() -> Unit
            page.func()
        }

        val content = page.content()
        val response = Response(request, Status.OK)
        response.contents["payload"] = content
        response.contents["context"] = context
        return response
    }

    fun getContext(request: Request): BrowserContext{
        return if(request.parameters.containsKey("context")){
            request.parameters["context"] as BrowserContext
        } else{
            browser.newContext()
        }
    }
}