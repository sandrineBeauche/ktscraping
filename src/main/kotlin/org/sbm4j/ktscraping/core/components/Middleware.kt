package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.*
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.logger

abstract class AbstractMiddleware(name: String):
    AbstractMiddleComponent(name),
    RequestForwarder,
    ResponseForwarder
{

    override suspend fun processResponse(response: Response) {
    }

    override suspend fun run() {
        super<RequestForwarder>.run()
        super<ResponseForwarder>.run()
        super<AbstractMiddleComponent>.run()
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
