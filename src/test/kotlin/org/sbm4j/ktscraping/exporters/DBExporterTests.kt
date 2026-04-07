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
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.db.NitriteDBConnexion
import org.sbm4j.meercat.nodes.logger
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

@Serializable
data class Address(
    val street: String,
    val number: Int,
    val zipCode: Int,
    val city: String
)

@Serializable
data class Contact(
    val contactId: Int,
    val firstname: String,
    val lastname: String,
    val years: Int,
    val address: Address? = null
): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}

class DBExporterTests: AbstractExporterTester<DBExporter>() {

    lateinit var db: NitriteDBConnexion

    lateinit var dbFile: File

    init {
        logger.debug { "setup nitrite exporter tester" }
        val uri = this.javaClass.getResource("/org.sbm4j.ktscraping/exporters/nitriteDB.db")?.toURI()!!
        dbFile = File(uri)
        db = NitriteDBConnexion(dbFile)
    }

    val data1 = Contact(1,"John", "Doe", 30)
    val data2 = Contact(2,"Mickey", "Mouse", 60,
        Address("rue des coquelicots", 3, 30000, "MickeyVille")
    )


    override fun buildNode(): DBExporter {
        val result = DBExporter("exporter")
        result.db = db
        return result
    }

    @BeforeEach
    fun setUpDB(){
        db.clear(Contact::class.java)
    }

    fun getFirstContact(): Contact{
        val contacts = db.getObjects(Contact::class.java)
        return contacts[0] as Contact
    }

    @AfterEach
    fun tearDownDB(){
        db.close()
    }

    @Test
    fun testExportItem1() = TestScope().runTest{

        val item = ObjectDataItem.build(data1, "test", sender)

        withConsumer {
            inChannel.send(item)
            val itemAck = inChannel.channel.receive()
        }

        val size = db.getSize(Contact::class.java)
        assertThat(size, equalTo(1))

        val cursor = db.getObjects(Contact::class.java)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun testExportItem2() = TestScope().runTest{

        val item = ObjectDataItem.build(data2, "test", sender)

        withConsumer {
            inChannel.send(item)
            val itemAck = inChannel.channel.receive()
        }

        val size = db.getSize(Contact::class.java)
        assertThat(size, equalTo(1))

        val cursor = db.getObjects(Contact::class.java)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun testUpdateItem() = TestScope().runTest {
        val item = ObjectDataItem.build(data1, "test", sender)

        val updateItem = ItemUpdate(
            Contact::class.java,
            Contact::contactId,
            1,
            mapOf("years" to 20),
            sender = sender
        )

        withConsumer {
            inChannel.send(item)
            val itemAck = inChannel.channel.receive()

            inChannel.send(updateItem)
            val itemAck2 = inChannel.channel.receive()
        }

        val cont = getFirstContact()
        assertThat(cont.years, equalTo(20))
        println(cont)
    }

    @Test
    fun testUpdateItem2() = TestScope().runTest {
        val item = ObjectDataItem.build(data2, "test", sender)

        val updateItem = ItemUpdate(
            Contact::class.java,
            Contact::contactId,
            2,
            mapOf("address.number" to 4),
            sender = sender
        )

        withConsumer {
            inChannel.send(item)
            val itemAck = inChannel.channel.receive()

            inChannel.send(updateItem)
            val itemAck2 = inChannel.channel.receive()
        }

        val cont = getFirstContact()
        assertThat(cont.address?.number, equalTo(4))
        println(cont)
    }

    @Test
    fun testDeleteItem() = TestScope().runTest {
        val deleteItem = ItemDelete(
            Contact::class.java,
            Contact::contactId,
            1,
            sender = sender
        )

        withConsumer {
            inChannel.send(ObjectDataItem.build(data1, "test", sender))
            val itemAck1 = inChannel.channel.receive()

            inChannel.send(ObjectDataItem.build(data2, "test", sender))

            val itemAck2 = inChannel.channel.receive()

            inChannel.send(deleteItem)
            val itemAck3 = inChannel.channel.receive()
        }

        val size = db.getSize(Contact::class.java)
        val contacts = db.getObjects(Contact::class.java)
        contacts.forEach {
            println(it)
        }

    }
}