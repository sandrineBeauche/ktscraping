package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.db.NitriteDBConnexion
import org.sbm4j.ktscraping.exporters.Address
import org.sbm4j.ktscraping.exporters.Contact
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.sbm4j.meercat.nodes.logger

class DBPipelineTests: AbstractPipelineTester<DBPipeline<*>>() {

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

    override fun buildNode(): DBPipeline<*> {
        val pipeline = DBPipeline<Contact>("db pipeline")
        pipeline.db = db
        pipeline.objectClass = Contact::class.java
        pipeline.inChannel = inChannel
        pipeline.outChannel = outChannel
        return pipeline
    }


    @BeforeTest
    fun setUpDB(){
        db.clear(Contact::class.java)
    }

    @Test
    fun testDBPipeline1() = TestScope().runTest{

        val item = ObjectDataItem.build(data1, "test", sender)

        withConsumer {
            inChannel.send(item)

            val end = EndEvent(sender)
            inChannel.send(end)

            val l = outChannel.getSendFlow(ObjectDataItem::class).take(2).toList()

            println(l)
        }

        val size = db.getSize(Contact::class.java)
        assertThat(size, equalTo(1))
    }
}