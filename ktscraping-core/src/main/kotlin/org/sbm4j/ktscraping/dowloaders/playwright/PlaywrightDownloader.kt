package org.sbm4j.ktscraping.dowloaders.playwright

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Cookie
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.middleware.CookiesMiddleware
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.concurrent.Executors

/**
 * A request that drives a Playwright browser page to scrape JavaScript-rendered content.
 *
 * Unlike [Request] which performs a simple HTTP download, [PlaywrightRequest] delegates
 * the download to a headless browser via the Playwright framework. The [func] lambda
 * runs in the context of a Playwright [Page], giving full access to the browser API
 * (navigation, clicking, waiting for elements, etc.) and populates the [results] map
 * with the extracted data.
 *
 * The [func] is stored in [parameters] under [PlaywrightDownloader.PLAYWRIGHT] so that
 * the [PlaywrightDownloader] can retrieve and execute it.
 *
 * @property sender The Spider emitting this request.
 * @property url The URL of the page to open in the browser.
 * @property func Lambda executed in the context of a Playwright [Page]. Receives a
 * mutable [results] map to populate with extracted data during browser interaction.
 *
 * @see PlaywrightDownloader
 * @see DownloadingRequest
 */
data class PlaywrightRequest(
    override var sender: SendSource,
    override var url: String,
    val func: Page.(results: MutableMap<String, Any>) -> Unit
): DownloadingRequest(sender, url){

    init {
        parameters[PlaywrightDownloader.PLAYWRIGHT] = func
    }

    /** Creates a copy of this request via [copy]. */
    override fun clone(): PlaywrightRequest {
        return this.copy()
    }

}


/**
 * An [AbstractDownloader] that performs browser-based downloads using Playwright.
 *
 * Unlike [HttpClientDownloader] which performs simple HTTP GET requests, [PlaywrightDownloader]
 * drives a real Chromium browser to handle JavaScript-rendered pages, complex navigation,
 * cookie management, and custom page interactions via [PlaywrightRequest.func].
 *
 * **Thread model**: Playwright is not thread-safe and must be used from the thread that
 * created it. [PlaywrightDownloader] manages two types of coroutine dispatchers, each
 * backed by [PlaywrightThread] instances:
 * - [cachedThreadPoolDispatcher]: a cached thread pool for stateless requests (no [CONTEXT_NAME]).
 *   Each request may run on any available Playwright thread.
 * - [namedThreadDispatcher]: a single-thread executor per named context, ensuring that all
 *   requests sharing a [CONTEXT_NAME] run on the same thread, preserving session state
 *   (cookies, local storage, etc.) across requests.
 *
 * Three standard keys are defined for use in [AbstractRequest.parameters] and
 * [DownloadingResponse.contents]:
 * - [PLAYWRIGHT]: the [PlaywrightRequest.func] lambda to execute on the page.
 * - [CONTEXT_NAME]: the name of the persistent browser context for session-aware requests.
 * - [RESULTS]: the map populated by [PlaywrightRequest.func] with extracted data.
 *
 * @param name The name of this downloader node, defaults to `"Playwright downloader"`.
 *
 * @see AbstractDownloader
 * @see PlaywrightThread
 * @see PlaywrightThreadFactory
 * @see PlaywrightRequest
 */
class PlaywrightDownloader(name: String = "Playwright downloader") : AbstractDownloader(name) {
    companion object{
        /** Key in [AbstractRequest.parameters] for the [PlaywrightRequest.func] lambda. */
        val PLAYWRIGHT: String = "playwright"

        /**
         * Key in [AbstractRequest.parameters] for the named browser context identifier.
         * When set, all requests with the same [CONTEXT_NAME] share a dedicated single-thread
         * dispatcher, preserving session state (cookies, local storage) across requests.
         */
        val CONTEXT_NAME: String = "contextName"

        /** Key in [DownloadingResponse.contents] for the results map populated by [PlaywrightRequest.func]. */
        val RESULTS: String = "results"
    }

    /** Whether to launch the Chromium browser in headless mode, defaults to `true`. */
    var headless: Boolean = true

    /** Thread factory creating [PlaywrightThread] instances for this downloader. */
    lateinit var factory: PlaywrightThreadfactory

    /**
     * Cached thread pool dispatcher for stateless requests.
     * Each request may run on any available [PlaywrightThread].
     */
    lateinit var cachedThreadPoolDispatcher: ExecutorCoroutineDispatcher

    /**
     * Map of named single-thread dispatchers, keyed by [CONTEXT_NAME].
     * Ensures session-aware requests always run on the same [PlaywrightThread].
     */
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

    /**
     * Performs a browser-based download for the given [request].
     *
     * Selects the appropriate dispatcher via [getDispatcher], then executes the download
     * on a [PlaywrightThread] using [withContext]. For each request:
     * 1. Opens a new page in the browser or a named context (if [CONTEXT_NAME] is set).
     * 2. Injects cookies from [CookiesMiddleware.COOKIE] if present.
     * 3. Navigates to [DownloadingRequest.url].
     * 4. Executes [PlaywrightRequest.func] if present, storing results under [RESULTS].
     * 5. Captures the full page content under [PAYLOAD].
     * 6. Captures frames under [FRAMES] if the page has multiple frames.
     * 7. Captures cookies from the browser context under [CookiesMiddleware.COOKIE].
     *
     * @param request The download request to process.
     * @return A [DownloadingResponse] populated with the page content and extracted data.
     */
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

    /**
     * Selects the appropriate coroutine dispatcher for the given [request].
     *
     * If [CONTEXT_NAME] is set in [AbstractRequest.parameters], returns a dedicated
     * single-thread dispatcher for that context (creating it if needed), ensuring
     * session state is preserved across requests. Otherwise, returns the shared
     * [cachedThreadPoolDispatcher].
     *
     * @param request The request to select a dispatcher for.
     * @return The [ExecutorCoroutineDispatcher] to use for this request.
     */
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

    /**
     * Infers the [ContentType] from the raw page content using heuristic detection.
     *
     * Since Playwright does not expose HTTP response headers directly, the content type
     * is determined by inspecting the content string:
     * - Contains `<html` → [ContentType.HTML]
     * - Starts with `<svg` → [ContentType.SVG_IMAGE]
     * - Contains `</` → [ContentType.XML]
     * - Starts with `{` or `[` → [ContentType.JSON]
     * - Otherwise → [ContentType.FILE]
     *
     * @param content The raw page content returned by Playwright.
     * @return The inferred [ContentType].
     */
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