package org.sbm4j.ktscraping.exporters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.sbm4j.ktscraping.core.utils.AbstractExporterTester
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.item.ItemUpdate
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.db.CollectionDBConnection
import org.sbm4j.ktscraping.db.clear
import org.sbm4j.ktscraping.db.getSize
import org.sbm4j.ktscraping.domain.Address
import org.sbm4j.ktscraping.domain.Contact
import org.sbm4j.ktscraping.utils.isOkItemAck
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test



class DBExporterTests: AbstractExporterTester<DBExporter>() {

    lateinit var db: CollectionDBConnection

    init {
        logger.debug { "setup collectionDB exporter tester" }
        db = CollectionDBConnection()
    }

    val data1 = Contact(1, "John", "Doe", 30)
    val data2 = Contact(2,"Mickey", "Mouse", 60,
        Address("rue des coquelicots", 3, 30000, "MickeyVille")
    )


    override fun buildNode(): DBExporter {
        val result = DBExporter("exporter")
        result.db = db
        result.inChannel = inChannel
        return result
    }

    @BeforeEach
    fun setUpDB(){
        db.clear<Contact>()
    }

    fun getFirstContact(): Contact{
        val contacts = db.getObjects(Contact::class)
        return contacts[0] as Contact
    }

    @AfterEach
    fun tearDownDB(){
        db.close()
    }

    @Test
    fun `db export simple object item`() = TestScope().runTest{

        val item = ObjectDataItem.build(data1, "test", sender)

        withConsumer {
            val itemAck = inChannel.sendSync<ItemAck>(item)
            assertThat(itemAck, isOkItemAck(item))
        }

        val size = db.getSize<Contact>()
        assertThat(size, equalTo(1))

        val cursor = db.getObjects(Contact::class)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun `db export complex object item`() = TestScope().runTest{

        val item = ObjectDataItem.build(data2, "test", sender)

        withConsumer {
            val itemAck = inChannel.sendSync<ItemAck>(item)
            assertThat(itemAck, isOkItemAck(item))
        }

        val size = db.getSize(Contact::class)
        assertThat(size, equalTo(1))

        val cursor = db.getObjects(Contact::class)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun `db export update on simple object item`() = TestScope().runTest {
        val item = ObjectDataItem.build(data1, "test", sender)

        val updateItem = ItemUpdate(
            Contact::class,
            Contact::contactId,
            1,
            mapOf("years" to 20),
            sender = sender
        )

        withConsumer {
            val itemAck = inChannel.sendSync<ItemAck>(item)
            assertThat(itemAck, isOkItemAck(item))

            val itemAck2 = inChannel.sendSync<ItemAck>(updateItem)
            assertThat(itemAck, isOkItemAck(item))
        }

        val cont = getFirstContact()
        assertThat(cont.years, equalTo(20))
        println(cont)
    }

    @Test
    fun `db export update on complex object item`() = TestScope().runTest {
        val item = ObjectDataItem.build(data2, "test", sender)

        val updateItem = ItemUpdate(
            Contact::class,
            Contact::contactId,
            2,
            mapOf("address.number" to 4),
            sender = sender
        )

        withConsumer {
            val itemAck = inChannel.sendSync<ItemAck>(item)
            val itemAck2 = inChannel.sendSync<ItemAck>(updateItem)
        }

        val cont = getFirstContact()
        assertThat(cont.address?.number, equalTo(4))
        println(cont)
    }

    @Test
    fun `db deletes item`() = TestScope().runTest {
        val deleteItem = ItemDelete(
            Contact::class,
            Contact::contactId,
            1,
            sender = sender
        )

        val item1 = ObjectDataItem.build(data1, "test", sender)
        val item2 = ObjectDataItem.build(data2, "test", sender)

        withConsumer {
            val itemAck1 = inChannel.sendSync<ItemAck>(item1)
            val itemAck2 = inChannel.sendSync<ItemAck>(item2)
            val itemAck3 = inChannel.sendSync<ItemAck>(deleteItem)
        }

        val size = db.getSize(Contact::class)
        val contacts = db.getObjects(Contact::class)
        contacts.forEach {
            println(it)
        }

    }
}