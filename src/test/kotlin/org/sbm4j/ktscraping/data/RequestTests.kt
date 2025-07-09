package org.sbm4j.ktscraping.data

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.mockk
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.data.request.Request
import kotlin.test.Test

class RequestTests {

    val sender: RequestSender = mockk<RequestSender>()

    @Test
    fun testExtractServerFromUrl(){
        val req = Request(sender, "http://server1/toto")
        val server = req.extractServerFromUrl()
        assertThat(server, equalTo("server1"))
    }

}