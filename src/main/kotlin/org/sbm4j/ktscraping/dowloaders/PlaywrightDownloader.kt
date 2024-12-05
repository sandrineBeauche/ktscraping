package org.sbm4j.ktscraping.dowloaders

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status


class PlaywrightDownloader(scope: CoroutineScope, name: String, val headless: Boolean = true) : AbstractDownloader(scope, name) {

    lateinit var browser: Browser

    override suspend fun start() {
        super.start()
        val playwright = Playwright.create()
        browser = playwright.chromium().launch(LaunchOptions().setHeadless(headless))
    }

    override suspend fun stop() {
        super.stop()
        browser.close()
    }

    override fun processRequest(request: AbstractRequest): Any? {
        val context = getContext(request)
        val page = context.newPage()
        page.navigate(request.url)

        if(request.parameters.containsKey("playwright")){
            val func = request.parameters["playwright"] as Page.() -> Unit
            page.func()
        }

        val response = Response(request, Status.OK)
        response.contents["payload"] = page.content()
        if(page.frames().size > 1){
            val frames = page.frames().associateBy({it.name()}, {it.content()})
            response.contents["frames"] = frames
        }
        response.contents["context"] = context
        return response
    }

    fun getContext(request: AbstractRequest): BrowserContext{
        return if(request.parameters.containsKey("context")){
            request.parameters["context"] as BrowserContext
        } else{
            browser.newContext()
        }
    }
}