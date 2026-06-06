package org.sbm4j.ktscraping.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasElement
import io.github.serpro69.kfaker.Faker
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.domain.Address
import org.sbm4j.ktscraping.domain.Contact
import org.sbm4j.ktscraping.domain.buildContact
import org.sbm4j.ktscraping.domain.buildInsertContact
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

abstract class AbstractDBConnectionTests<T: DBConnexion> {

    val sender = mockk<SendSource>()

    val faker = Faker()

    val testScope = TestScope()

    lateinit var conn: T


    @AfterEach
    fun tearDown() {
        resetConnection()
    }




    fun getRootResources(): String {
        val url = this.javaClass.getResource("/db/")
        return url?.path!!
    }

    abstract fun buildDBConnection(): T

    open fun resetConnection(){}




    @Test
    fun `insert item`() = testScope.runTest {
        val before = conn.getSize<Contact>()

        val newContact = buildContact(3, 60)
        val item = buildInsertContact(newContact, sender)
        val result = conn.performInsertItem(item)
        conn.commit()

        assertTrue(result)
        val size = conn.getSize<Contact>()
        val keys = conn.getKeys<Contact>(Contact::lastname)
        assertThat(size, equalTo(before + 1L))
        assertThat(keys, hasElement(newContact.lastname))

    }



    @Test
    fun `update item`() = testScope.runTest {
        val item = ItemUpdate(
            Contact::class,
            Contact::contactId, 0,
            mapOf("years" to 21),
            sender = sender
        )
        val result = conn.performItemUpdate(item)
        conn.commit()


        val obj = conn.getObjects<Contact>().filter { it.contactId == 0 }[0]
        assertThat(obj, has(Contact::years, equalTo(21)))
        assertTrue(result)
    }

    @Test
    fun `update nested item`() = testScope.runTest {
        val item = ItemUpdate(
            Contact::class,
            Contact::contactId, 2,
            mapOf("address.zipCode" to 21000),
            sender = sender
        )
        val result = conn.performItemUpdate(item)
        conn.commit()

        val obj = conn.getObjects<Contact>().filter { it.contactId == 2 }[0]
        assertThat(obj.address!!, has(Address::zipCode, equalTo(21000)))
        assertTrue(result)
    }

    @Test
    fun `delete item`() = testScope.runTest {
        val item = ItemDelete(
            Contact::class,
            Contact::contactId, 0,
            sender = sender
        )
        val result = conn.performItemDelete(item)
        conn.commit()

        val size = conn.getSize<Contact>()
        assertThat(size, equalTo(2L))
        assertTrue(result)
    }
}