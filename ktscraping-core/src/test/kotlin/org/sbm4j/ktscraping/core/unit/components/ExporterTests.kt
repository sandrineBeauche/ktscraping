package org.sbm4j.ktscraping.core.unit.components

import com.natpryce.hamkrest.assertion.assertThat
import kotlinx.coroutines.test.runTest
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.utils.AbstractExporterTester
import org.sbm4j.ktscraping.core.utils.IntDataItem
import org.sbm4j.ktscraping.utils.isOkItemAck
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test

class TestingExporter : AbstractExporter("exporter") {
    override suspend fun exportItem(item: Item) {
        logger.info{"${name}: exports the item ${item}"}
    }

}

class ExporterTests(): AbstractExporterTester<TestingExporter>() {

    override fun buildNode(): TestingExporter {
        val result = TestingExporter()
        result.inChannel = inChannel
        return result
    }

    @Test
    fun `export a int item`() = testScope.runTest {
        val item = IntDataItem(1, sender)
        lateinit var ack: ItemAck

        withConsumer {
            ack = inChannel.sendSync<ItemAck>(item)
        }

        assertThat(ack, isOkItemAck(item))
    }

}