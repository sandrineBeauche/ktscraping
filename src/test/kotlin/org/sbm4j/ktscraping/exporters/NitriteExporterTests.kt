package org.sbm4j.ktscraping.exporters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.dizitart.kno2.filters.eq
import org.dizitart.no2.Nitrite
import org.sbm4j.ktscraping.core.AbstractExporter
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.core.utils.AbstractExporterTester
import org.sbm4j.ktscraping.requests.Data
import org.sbm4j.ktscraping.requests.DataItem
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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

class NitriteExporterTests: AbstractExporterTester() {

    lateinit var db: Nitrite

    val data1 = Contact(1,"John", "Doe", 30)
    val data2 = Contact(2,"Mickey", "Mouse", 60,
        Address("rue des coquelicots", 3, 30000, "MickeyVille")
    )

    override fun buildExporter(exporterName: String): AbstractExporter {
        val result = NitriteExporter(exporterName)
        result.db = db
        return result
    }


    @BeforeTest
    override fun setUp(){
        logger.debug { "setup nitrite exporter tester" }
        val uri = this.javaClass.getResource("/org.sbm4j.ktscraping/exporters/nitriteDB.db")?.toURI()!!
        val dbFile = File(uri)
        db = buildNitriteDB(dbFile)
        val repository = db.getRepository(Contact::class.java)
        repository.clear()
        super.setUp()
    }

    @AfterTest
    fun tearDown(){
        db.close()
    }

    @Test
    fun testExportItem1() = TestScope().runTest{

        val item = DataItem.build(data1, "test")

        withExporter {
            inChannel.send(item)
            val itemAck = outChannel.receive()
        }

        val repository = db.getRepository(Contact::class.java)
        assertThat(repository.size(), equalTo(1))

        val cursor = repository.find(Contact::contactId eq 1)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun testExportItem2() = TestScope().runTest{

        val item = DataItem.build(data2, "test")

        withExporter {
            inChannel.send(item)
            val itemAck = outChannel.receive()
        }

        val repository = db.getRepository(Contact::class.java)
        assertThat(repository.size(), equalTo(1))

        val cursor = repository.find(Contact::contactId eq 2)
        cursor.forEach {
            println(it)
        }
    }

    @Test
    fun testUpdateItem() = TestScope().runTest {
        val item = DataItem.build(data1, "test")

        val updateItem = ItemUpdate(
            Contact::class.java,
            Contact::contactId,
            1,
            mapOf("years" to 20)
        )

        withExporter {
            inChannel.send(item)
            val itemAck = outChannel.receive()

            inChannel.send(updateItem)
            val itemAck2 = outChannel.receive()
        }

        val repository = db.getRepository(Contact::class.java)
        val cursor = repository.find(Contact::contactId eq 1)
        val cont = cursor.first()
        assertThat(cont.years, equalTo(20))
        println(cont)
    }

    @Test
    fun testUpdateItem2() = TestScope().runTest {
        val item = DataItem.build(data2, "test")

        val updateItem = ItemUpdate(
            Contact::class.java,
            Contact::contactId,
            2,
            mapOf("address.number" to 4)
        )

        withExporter {
            inChannel.send(item)
            val itemAck = outChannel.receive()

            inChannel.send(updateItem)
            val itemAck2 = outChannel.receive()
        }

        val repository = db.getRepository(Contact::class.java)
        val cursor = repository.find(Contact::contactId eq 2)
        val cont = cursor.first()
        assertThat(cont.address?.number, equalTo(4))
        println(cont)
    }

    @Test
    fun testDeleteItem() = TestScope().runTest {
        val deleteItem = ItemDelete(
            Contact::class.java,
            Contact::contactId,
            1
        )

        withExporter {
            inChannel.send(DataItem.build(data1, "test"))
            val itemAck1 = outChannel.receive()

            inChannel.send(DataItem.build(data2, "test"))
            val itemAck2 = outChannel.receive()

            inChannel.send(deleteItem)
            val itemAck3 = outChannel.receive()
        }

        val repository = db.getRepository(Contact::class.java)
        assertThat(repository.size(), equalTo(1))
        val cursor = repository.find(Contact::contactId eq 2)
        cursor.forEach {
            println(it)
        }

    }
}