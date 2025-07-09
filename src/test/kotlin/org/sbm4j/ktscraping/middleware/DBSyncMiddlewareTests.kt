package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.exporters.Contact
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ItemError
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse

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
        request1.parameters["DBSyncKey"] = 2

        lateinit var response: DownloadingResponse
        lateinit var delete1: ItemDelete
        lateinit var delete2: ItemDelete
        lateinit var end: EndItem

        withMiddleware {
            inChannel.send(request1)
            response = forwardOutChannel.receive() as DownloadingResponse
            logger.debug { "Received a response: $response" }

            logger.debug { "send item end" }
            itemChannelIn.send(EndItem())

            logger.debug { "receive item to delete" }
            delete1 = itemChannelOut.receive() as ItemDelete
            logger.debug { "received item delete: $delete1" }
            delete2 = itemChannelOut.receive() as ItemDelete
            logger.debug { "received item delete: $delete2" }

            logger.debug { "receive followed item end" }
            end = itemChannelOut.receive() as EndItem
            logger.debug { "received followed item end" }
        }


    }

    @Test
    fun testDBSyncMiddleware2() = TestScope().runTest {

        val request1 = Request(sender, "")
        request1.parameters["DBSyncKey"] = 2

        lateinit var response: DownloadingResponse
        lateinit var end: EndItem

        withMiddleware {
            inChannel.send(request1)
            response = forwardOutChannel.receive() as DownloadingResponse
            logger.debug { "Received a response: $response" }

            val errorInfos = ErrorInfo(Exception(), this.middleware, ErrorLevel.MAJOR)
            itemChannelIn.send(ItemError(errorInfos))
            itemChannelOut.receive()

            logger.debug { "send item end" }
            itemChannelIn.send(EndItem())

            logger.debug { "receive followed item end" }
            end = itemChannelOut.receive() as EndItem
            logger.debug { "received followed item end" }
        }
    }
}