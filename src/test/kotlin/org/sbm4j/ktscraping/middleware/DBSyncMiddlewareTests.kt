package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.internal.ErrorLevel
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.exporters.Contact
import org.sbm4j.ktscraping.exporters.ItemDelete

class DBSyncMiddlewareTests: AbstractSpiderMiddlewareTester() {
    override fun buildMiddleware(middlewareName: String): SpiderMiddleware {
        val result = DBSyncMiddleware<Contact>(middlewareName)
        result.keys = setOf(1,2,3)
        result.classObject = Contact::class.java
        result.keyProperty = Contact::contactId
        return result
    }

    @Test
    fun testDBSyncMiddleware1() = TestScope().runTest {

        val request1 = Request(sender, "")
        request1.parameters[DBSyncMiddleware.DBSYNC_KEY] = 2

        lateinit var response: DownloadingResponse
        lateinit var delete1: ItemDelete
        lateinit var delete2: ItemDelete

        withMiddleware {
            inChannel.send(request1)
            response = outChannel.channel.receive() as DownloadingResponse
            logger.debug { "Received a response: $response" }

            logger.debug { "send end request" }
            inChannel.send(EndEvent(sender))

            logger.debug { "receive item to delete" }
            delete1 = outChannel.channel.receive() as ItemDelete
            logger.debug { "received item delete: $delete1" }
            delete2 = outChannel.channel.receive() as ItemDelete
            logger.debug { "received item delete: $delete2" }

            logger.debug { "receive followed item end" }
            outChannel.channel.receive() as EndEvent
            logger.debug { "received followed item end" }
        }


    }

    @Test
    fun testDBSyncMiddleware2() = TestScope().runTest {

        val request1 = Request(sender, "")
        request1.parameters["DBSyncKey"] = 2

        lateinit var response: DownloadingResponse
        lateinit var end: EndEvent

        withMiddleware {
            inChannel.send(request1)
            response = outChannel.channel.receive() as DownloadingResponse
            logger.debug { "Received a response: $response" }

            val errorInfos = ErrorInfo(Exception(), this.middleware, ErrorLevel.MAJOR)
            outChannel.send(ErrorInternal(errorInfos, sender))
            outChannel.channel.receive()

            logger.debug { "send item end" }
            inChannel.channel.send(EndEvent(sender))

            logger.debug { "receive followed item end" }
            end = outChannel.channel.receive() as EndEvent
            logger.debug { "received followed item end" }
        }
    }
}