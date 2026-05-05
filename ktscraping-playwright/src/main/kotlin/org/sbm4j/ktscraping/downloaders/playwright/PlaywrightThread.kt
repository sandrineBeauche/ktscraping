package org.sbm4j.ktscraping.downloaders.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException

import org.sbm4j.meercat.nodes.logger
import java.util.concurrent.ThreadFactory

/**
 * A [ThreadFactory] that creates [PlaywrightThread] instances for Playwright-based downloading.
 *
 * Since Playwright is not thread-safe and must be used from the thread that created it,
 * each thread manages its own isolated Playwright instance. Multiple [PlaywrightThread]s
 * can run in parallel to perform concurrent browser-based downloads.
 *
 * @param headless Whether to launch the browser in headless mode, defaults to `true`.
 *
 * @see PlaywrightThread
 * @see PlaywrightDownloader
 */
class PlaywrightThreadfactory(val headless: Boolean = true): ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        return PlaywrightThread(headless, r)
    }

}

/**
 * A dedicated [Thread] that owns and manages a single Playwright browser instance.
 *
 * Each [PlaywrightThread] creates its own [Playwright], [Browser], and [BrowserContext]
 * on startup, executes the provided [runnable], then properly closes all resources in
 * order (context → browser → Playwright) when the runnable completes.
 *
 * This design ensures Playwright thread-safety: since each instance is confined to its
 * own thread, multiple [PlaywrightThread]s can run concurrently to perform parallel
 * browser-based downloads without interference.
 *
 * [PlaywrightException] during Playwright shutdown is caught and logged as a warning,
 * since Playwright may already be shutting down at that point.
 *
 * @param headless Whether to launch the Chromium browser in headless mode.
 * @param runnable The task to execute within the browser context.
 *
 * @see PlaywrightThreadFactory
 * @see PlaywrightDownloader
 */
class PlaywrightThread(val headless: Boolean, val runnable: Runnable): Thread(){

    /** The Playwright instance owned by this thread. Initialized on [run]. */
    lateinit var playwright: Playwright

    /** The Chromium browser instance owned by this thread. Initialized on [run]. */
    lateinit var browser: Browser

    /** The browser context for this thread's scraping session. Initialized on [run]. */
    lateinit var context: BrowserContext

    /**
     * Creates the Playwright instance, launches a Chromium browser, opens a [BrowserContext],
     * executes [runnable], then closes all resources in reverse order.
     *
     * Any exception thrown by [runnable] is caught and logged as an error to prevent
     * unhandled thread crashes.
     */
    override fun run() {
        try {
            val playwright = Playwright.create()
            browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(headless))
            context = browser.newContext()

            runnable.run()


            context.close()
            browser.close()
            try{
                logger.info{"$name: try to close Playwright"}
                playwright.close()
            }
            catch(ex: PlaywrightException){
                ex.printStackTrace()
                logger.warn { "$name: Playwright is already shutdown" }
            }
        }
        catch(ex:Exception){
            logger.error(ex) { ex.message }
        }
    }

}