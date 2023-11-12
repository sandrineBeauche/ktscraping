package org.sbm4j.ktscraping.core

abstract class AbstractExporter: ItemReceiver {

    override suspend fun start() {
        logger.info{"Starting Exporter ${name}"}
        this.performItems()
    }

}