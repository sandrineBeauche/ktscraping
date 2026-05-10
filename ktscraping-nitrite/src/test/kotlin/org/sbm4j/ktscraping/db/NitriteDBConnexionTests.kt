package org.sbm4j.ktscraping.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import io.github.serpro69.kfaker.Faker
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.io.File
import java.net.URL
import kotlin.test.Test

@Serializable
data class Contact(
    val id: Long,
    val firstname: String,
    val lastname: String,
    val age: Int
): Data(){
    override fun clone(): Data {
        return this.copy()
    }
}

class NitriteDBConnexionTests {

    val sender = mockk<SendSource>()

    val faker = Faker()

    val testScope = TestScope()

    lateinit var temporaryDBFile: File

    lateinit var conn: NitriteDBConnexion

    fun getRootResources(): String {
        val url = this.javaClass.getResource("/nitrite/")
        return url?.path!!
    }

    fun generateDB(){
        val ages = listOf(20, 35, 40)
        val items = ages.mapIndexed { index, i ->
            buildInsertContact(index.toLong(), i)
        }

        items.forEach {
            conn.perfomInsertItem(it)
        }
    }

    @BeforeEach
    fun setUp() {
        val f = File(getRootResources(), "contacts.nitrite")
        temporaryDBFile = File(getRootResources(), "tempContacts.nitrite.db")
        f.copyTo(temporaryDBFile)
        conn = NitriteDBConnexion(temporaryDBFile)
    }

    @AfterEach
    fun tearDown() {
        conn.close()
        NitriteDBConnexion.reset()
        temporaryDBFile.delete()
    }

    fun buildInsertContact(id: Long, age: Int): ObjectDataItem<Contact> {
        val firstname = faker.name.firstName()
        val lastname = faker.name.lastName()
        val data = Contact(id, firstname, lastname, age)
        return ObjectDataItem(data, Contact::class, "contact", sender)
    }

    @Test
    fun testInsert() = testScope.runTest {
        val before = conn.getSize(Contact::class.java)

        val item = buildInsertContact(3, 60)
        conn.perfomInsertItem(item)
        conn.commit()

        val size = conn.getSize(Contact::class.java)
        val keys = conn.getKeys(Contact::class.java, Contact::lastname)
        assertThat(size, equalTo(before + 1L))
        assertThat(keys.size.toLong(), equalTo(before + 1))
    }

    @Test
    fun testUpdate() = testScope.runTest {
        val item = ItemUpdate(
            Contact::class.java,
            Contact::id, 0L,
            mapOf("age" to 21),
            sender = sender
        )
        conn.performItemUpdate(item)
        conn.commit()

        val obj = conn.getObjects(Contact::class.java).filter { it.id == 0L }[0]
        assertThat(obj, has(Contact::age, equalTo(21)))
    }

    @Test
    fun testDelete() = testScope.runTest {
        val item = ItemDelete(
            Contact::class.java,
            Contact::id, 0L,
            sender = sender
        )
        conn.performItemDelete(item)
        conn.commit()

        val size = conn.getSize(Contact::class.java)
        assertThat(size, equalTo(2L))
    }
}