package org.sbm4j.ktscraping.requests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.utils.AbstractPlaywrightRequestDownloadTester
import kotlin.test.Test

class GoogleSearchRequestTests: AbstractPlaywrightRequestDownloadTester(true) {

    @Test
    fun testGoogleSearchRequest() = TestScope().runTest {
        val request = GoogleSearchRequest(sender, "Gaston Lagaffe", 1)
        val response = sendRequest(request)
        val paylaod = response?.contents?.get("payload") as String

        val expected = "src=\"data:image/jpeg;base64"

        assertThat(paylaod, containsSubstring(expected))
    }
}