package org.sbm4j.bibliRennesManager.scraper

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.utils.AbstractDownloaderTester
import org.sbm4j.ktscraping.dowloaders.PlaywrightDownloader
import org.sbm4j.ktscraping.requests.PlaywrightRequest
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.test.assertNotNull
import kotlin.time.Duration

class LoginAccountTests: AbstractDownloaderTester() {
    override fun buildDownloader(sc: CoroutineScope, downloaderName: String): AbstractDownloader {
        return PlaywrightDownloader(sc, downloaderName, false)
    }

    @Test
    fun testSimpleLogin() = TestScope().runTest(timeout = Duration.ZERO) {
        val request = PlaywrightRequest(sender, "https://opac.si.leschampslibres.fr/iii/encore/myaccount?lang=frf"){
            this.locator("input#code").fill("23500002705434")
            this.locator("input#pin").fill("9ewxxjIUAfLcYGIKY1CT")
            this.locator("input[name=\"submit\"]").click()
            this.waitForSelector("div.accountSummary")
        }

        val response = sendRequest(request)

        assertNotNull(response)
    }
}