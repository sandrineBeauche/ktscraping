package org.sbm4j.ktscraping.middleware

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.isEmpty
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.core.utils.AbstractSpiderMiddlewareTester
import org.sbm4j.ktscraping.core.utils.ComponentStub
import org.sbm4j.ktscraping.data.events.DBSyncEvent
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.db.CollectionDBConnection
import org.sbm4j.ktscraping.domain.Contact
import org.sbm4j.ktscraping.domain.buildDBItems
import org.sbm4j.ktscraping.domain.buildInsertContact
import org.sbm4j.ktscraping.utils.isOKEventBackWith
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.nodes.logger

class DBSyncMiddlewareTests: AbstractSpiderMiddlewareTester<DBSyncMiddleware<Contact>>() {

    val dbConnexion: CollectionDBConnection = CollectionDBConnection()

    @BeforeEach
    fun setupDB(): Unit = runBlocking {
        val contacts = buildDBItems(listOf(20, 35, 40))
        dbConnexion.data.addAll(contacts)
    }

    @AfterEach
    fun cleanupDB(): Unit = runBlocking {
        dbConnexion.data.clear()
    }

    override fun buildNode(): DBSyncMiddleware<Contact> {
        val result = DBSyncMiddleware<Contact>("DB sync middleware")
        result.classObject = Contact::class
        result.keyProperty = Contact::contactId
        result.inChannel = inChannel
        result.outChannel = outChannel
        result.dbConnexion = dbConnexion
        return result
    }

    fun buildRequestDBSync(id: Int): Request{
        val request = Request(sender, "")
        request.parameters[DBSyncMiddleware.DBSYNC_KEY] = id
        return request
    }

    @Test
    fun `all up to date and 1 to delete`() = TestScope().runTest {

        val request1 = buildRequestDBSync(0)
        val request2 = buildRequestDBSync(2)
        val syncEvent = DBSyncEvent(sender, Contact::class)

        withConsumer {
            val response1 = inChannel.sendSync<DownloadingResponse>(request1)
            logger.debug { "Received a response for request 1: $response1" }
            assertThat(response1.contents[DBSyncMiddleware.DBSYNC_STATE],
                equalTo(DBSyncState.UPTODATE))

            val response2 = inChannel.sendSync<DownloadingResponse>(request2)
            logger.debug { "Received a response for request 1: $response2" }
            assertThat(response1.contents[DBSyncMiddleware.DBSYNC_STATE],
                equalTo(DBSyncState.UPTODATE))

            val back = inChannel.sendSync<EventBack>(syncEvent)
            logger.debug { "Received a back for sync event: $back" }

            val items = getReceivedItem()

            assertThat(back, isOKEventBackWith("sync"))
            assertThat(items, hasSize(equalTo(1)))

            assertThat(items[0], isA<ItemDelete<*>>(
                has(ItemDelete<*>::keyValue, equalTo(1))
            ))

            coVerify(exactly = 0) { (stub as ComponentStub).performRequest(any()) }
        }

    }


    @Test
    fun `new item`() = TestScope().runTest {

        val request1 = buildRequestDBSync(3)

        withConsumer {
            val response = inChannel.sendSync<DownloadingResponse>(request1)
            logger.debug { "Received a response: $response" }

            assertThat(response.contents[DBSyncMiddleware.DBSYNC_STATE],
                equalTo(DBSyncState.NEW))

            val requests = getReceivedRequest()
            assertThat(requests, hasSize(equalTo(1)))

        }
    }
}