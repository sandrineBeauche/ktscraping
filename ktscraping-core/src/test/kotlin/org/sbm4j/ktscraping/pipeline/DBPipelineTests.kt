package org.sbm4j.ktscraping.pipeline

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.utils.AbstractPipelineTester
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.db.CollectionDBConnection
import org.sbm4j.ktscraping.domain.Address
import org.sbm4j.ktscraping.domain.Contact
import org.sbm4j.meercat.nodes.logger
import kotlin.test.BeforeTest
import kotlin.test.Test

class DBPipelineTests: AbstractPipelineTester<DBPipeline<*>>() {

    lateinit var db: CollectionDBConnection


    init {
        logger.debug { "setup nitrite exporter tester" }
        db = CollectionDBConnection()
    }

    val data1 = Contact(1, "John", "Doe", 30)
    val data2 = Contact(2,"Mickey", "Mouse", 60,
        Address("rue des coquelicots", 3, 30000, "MickeyVille")
    )

    override fun buildNode(): DBPipeline<*> {
        val pipeline = DBPipeline<Contact>("db pipeline")
        pipeline.db = db
        pipeline.objectClass = Contact::class
        pipeline.inChannel = inChannel
        pipeline.outChannel = outChannel
        return pipeline
    }


    @BeforeTest
    fun setUpDB(){
        db.clear(Contact::class)
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

        val size = db.getSize(Contact::class)
        assertThat(size, equalTo(1))
    }
}