package org.sbm4j.ktscraping.core.components

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.EventBackForwarder
import org.sbm4j.meercat.components.EventConsumer
import org.sbm4j.ktscraping.core.processors.ItemForwarder
import org.sbm4j.ktscraping.core.processors.RequestForwarder
import org.sbm4j.ktscraping.core.processors.ResponseForwarder
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.components.AbstractControllable
import org.sbm4j.meercat.components.logger

abstract class AbstractMiddleware(override val name: String):
    AbstractControllable(),
    RequestForwarder,
    ResponseForwarder,
    EventConsumer,
    EventBackForwarder
{

    override lateinit var inChannel: SuperChannel

    override lateinit var outChannel: SuperChannel


    override suspend fun processResponse(response: Response) {
    }

    override suspend fun run() {
        super<EventConsumer>.run()
        super<EventBackForwarder>.run()
        super<RequestForwarder>.run()
        super<ResponseForwarder>.run()
    }
}
/**
 *
 */
abstract class SpiderMiddleware(
    name:String
): AbstractMiddleware(name), ItemForwarder
{

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        super<AbstractMiddleware>.run()
        super<ItemForwarder>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping spider middleware"}
        super<AbstractMiddleware>.stop()

    }

}

abstract class DownloaderMiddleware(name: String) :
    AbstractMiddleware(name)
{

    override suspend fun run() {
        logger.info{"${name}: Starting downloader middleware"}
        super.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader middleware"}
        super.stop()
    }

}
