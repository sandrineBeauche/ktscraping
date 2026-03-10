package org.sbm4j.ktscraping.core.components

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.EventBackForwarder
import org.sbm4j.meercat.components.EventConsumer
import org.sbm4j.ktscraping.core.processors.ItemAckForwarder
import org.sbm4j.ktscraping.core.processors.ItemForwarder
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.logger


interface Pipeline : ItemForwarder, ItemAckForwarder, EventConsumer, EventBackForwarder {

    override suspend fun run() {
        logger.info { "${name}: Starting pipeline" }
        super<ItemForwarder>.run()
        super<ItemAckForwarder>.run()
        super<EventConsumer>.run()
        super<EventBackForwarder>.run()

    }

    override suspend fun stop() {
        logger.info { "${name}: Stopping pipeline" }
        super<ItemForwarder>.stop()
        super<ItemAckForwarder>.stop()
        super<EventConsumer>.stop()
        super<EventBackForwarder>.stop()
    }

}

abstract class AbstractPipeline(override var name: String) : Pipeline, AbstractControllable() {

    override lateinit var inChannel: SuperChannel

    override lateinit var outChannel: SuperChannel
}