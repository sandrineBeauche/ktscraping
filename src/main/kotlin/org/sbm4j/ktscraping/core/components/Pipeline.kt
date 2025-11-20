package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.ItemAckForwarder
import org.sbm4j.ktscraping.core.processors.ItemForwarder


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