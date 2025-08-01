package org.sbm4j.ktscraping.dowloaders.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import org.sbm4j.ktscraping.core.logger
import java.util.concurrent.ThreadFactory

class PlaywrightThreadfactory(val headless: Boolean = true): ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        return PlaywrightThread(headless, r)
    }

}

class PlaywrightThread(val headless: Boolean, val runnable: Runnable): Thread(){

    lateinit var playwright: Playwright

    lateinit var browser: Browser

    lateinit var context: BrowserContext
    

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