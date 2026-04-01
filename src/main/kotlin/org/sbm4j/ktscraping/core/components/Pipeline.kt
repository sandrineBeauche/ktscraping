package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.*
import org.sbm4j.meercat.nodes.logger
import java.util.concurrent.ConcurrentHashMap


interface Pipeline : ItemForwarder, ItemAckForwarder{

    override suspend fun run() {
        logger.info { "${name}: Starting pipeline" }
        super<ItemForwarder>.run()
        super<ItemAckForwarder>.run()
    }

    override suspend fun stop() {
        logger.info { "${name}: Stopping pipeline" }
        super<ItemForwarder>.stop()
        super<ItemAckForwarder>.stop()
    }

}

abstract class AbstractPipeline(name: String) : Pipeline, AbstractMiddleComponent(name) {

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>
            = ConcurrentHashMap()

    override suspend fun run() {
        super<Pipeline>.run()
        super<AbstractMiddleComponent>.run()
    }
}