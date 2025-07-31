package org.sbm4j.ktscraping.dowloaders.playwright

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Cookie
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.middleware.CookiesMiddleware
import java.util.concurrent.Executors


open class PlaywrightRequest(
    override val sender: RequestSender,
    override var url: String,
    func: Page.(results: MutableMap<String, Any>) -> Unit
): DownloadingRequest(sender, url){

    init {
        parameters[PlaywrightDownloader.PLAYWRIGHT] = func
    }

}

class PlaywrightDownloader(name: String = "Playwright downloader") : AbstractDownloader(name) {
    companion object{
        val PLAYWRIGHT: String = "playwright"
        val CONTEXT_NAME: String = "contextName"
        val RESULTS: String = "results"
    }

    var headless: Boolean = true

    lateinit var factory: PlaywrightThreadfactory

    lateinit var cachedThreadPoolDispatcher: ExecutorCoroutineDispatcher

    val namedThreadDispatcher: MutableMap<String, ExecutorCoroutineDispatcher> = mutableMapOf()


    override suspend fun run() {
        super.run()
        factory = PlaywrightThreadfactory(headless)
        cachedThreadPoolDispatcher = Executors.newCachedThreadPool(factory).asCoroutineDispatcher()
    }

    override suspend fun stop() {
        cachedThreadPoolDispatcher.close()
        namedThreadDispatcher.values.forEach { it.close() }
        super.stop()
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        val dispatcher = getDispatcher(request)
        val result = withContext(dispatcher){
            val th = (Thread.currentThread() as PlaywrightThread)
            logger.debug { "${this@PlaywrightDownloader.name}: process request on thread ${th.name}" }
            val page = if(request.parameters.containsKey(CONTEXT_NAME)){
                th.context.newPage()
            }
            else{
                th.browser.newPage()
            }

            val cookies = request.parameters[CookiesMiddleware.COOKIE] as MutableList<*>?
            if(cookies != null){
                page.context().addCookies(cookies as MutableList<Cookie>)
            }


            page.navigate(request.url)

            val response = DownloadingResponse(request)

            if(request.parameters.containsKey(PLAYWRIGHT)){
                val results = mutableMapOf<String, Any>()
                val func = request.parameters[PLAYWRIGHT] as Page.(results: MutableMap<String, Any>) -> Unit
                page.func(results)
                if(results.isNotEmpty()) {
                    response.contents[RESULTS] = results
                }
            }

            val content = page.content()
            response.contents[PAYLOAD] = content
            response.type = getContentType(content)

            if(page.frames().size > 1){
                val frames = page.frames().associateBy({it.name()}, {it.content()})
                response.contents[FRAMES] = frames
            }

            response.contents[CookiesMiddleware.COOKIE] = th.context.cookies()

            page.close()
            logger.trace { "${this@PlaywrightDownloader.name}: finished process request on thread ${th.name}" }
            response
        }
        return result
    }

    fun getDispatcher(request: AbstractRequest): ExecutorCoroutineDispatcher{
        val name = request.parameters[CONTEXT_NAME] as String?
        return if(name != null){
            namedThreadDispatcher.getOrPut(name){
                Executors.newSingleThreadExecutor(factory).asCoroutineDispatcher()
            }
        } else{
            cachedThreadPoolDispatcher
        }
    }

    fun getContentType(content: String): ContentType{
        if(content.contains("<html")){
            return ContentType.HTML
        }
        if(content.startsWith("<svg")){
            return ContentType.SVG_IMAGE
        }
        if(content.contains("</")){
            return ContentType.XML
        }
        if(content.startsWith("{") || content.startsWith("[")){
            return ContentType.JSON
        }
        return ContentType.FILE
    }
}