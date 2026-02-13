package org.sbm4j.ktscraping.core.unit.components

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.logger
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.core.utils.AbstractExporterTester
import org.sbm4j.ktscraping.core.utils.DataItemTest
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import kotlin.test.Test
import kotlin.test.assertSame

class ExporterTests(): AbstractExporterTester() {

    override fun buildExporter(exporterName: String): AbstractExporter {
        return object: AbstractExporter(exporterName){
            override suspend fun exportItem(item: Item) {
                logger.info{"${name}: exports the item ${item}"}
            }

            override suspend fun preStart(event: Event): EventJobResult? {
                logger.info { "${name}: inside pre start" }
                return null
            }

            override suspend fun preEnd(event: Event): EventJobResult? {
                logger.info { "${name}: inside pre end" }
                return null
            }
        }
    }


    @Test
    fun testExporter() = TestScope().runTest {
        val data = DataItemTest("value1", "req1")
        val item = ObjectDataItem<DataItemTest>(data, DataItemTest::class, sender = sender)
        lateinit var ack: ItemAck

        withExporter {
            ack = inChannel.sendSync<ItemAck>(item)
        }

        assertSame(item, ack.send)
    }

}