package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.EventBackForwarder
import org.sbm4j.ktscraping.core.processors.EventConsumer
import org.sbm4j.ktscraping.core.processors.RequestForwarder
import org.sbm4j.ktscraping.core.processors.ResponseForwarder
import org.sbm4j.ktscraping.data.response.Response

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
        TODO("Not yet implemented")
    }

    override suspend fun run() {
        super<RequestForwarder>.run()
        super<RequestForwarder>.run()
        super<EventConsumer>.run()
        super<EventBackForwarder>.run()
    }
}
/**
 *
 */
abstract class SpiderMiddleware(
    name:String
): AbstractMiddleware(name)
{

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        super.run()
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
        logger.info{"${name}: Stopping spider middleware"}
        super.stop()
    }




}
