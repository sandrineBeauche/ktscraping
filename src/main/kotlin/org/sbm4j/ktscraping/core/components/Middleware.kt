package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.*
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.AbstractMiddleNode
import org.sbm4j.meercat.nodes.logger

/**
 * Base abstract class for middleware nodes in the KtScraping topology.
 *
 * A middleware sits between two nodes in a branch and can intercept both outgoing
 * [AbstractRequest] messages (via [RequestForwarder]) and incoming [Response] backs
 * (via [ResponseForwarder]). The default [processResponse] implementation is a no-op,
 * allowing subclasses to override only what they need.
 *
 * On startup, [run] concurrently registers request forwarding, response forwarding,
 * and the standard middle component processing (event lifecycle).
 *
 * @param name The name of this middleware node.
 *
 * @see RequestForwarder
 * @see ResponseForwarder
 * @see AbstractMiddleComponent
 * @see SpiderMiddleware
 * @see DownloaderMiddleware
 */
abstract class AbstractMiddleware(name: String):
    AbstractMiddleComponent(name),
    RequestForwarder,
    ResponseForwarder
{
    /** No-op by default. Override to process or transform incoming [Response] backs. */
    override suspend fun processResponse(response: Response) {
    }

    override suspend fun run() {
        super<RequestForwarder>.run()
        super<ResponseForwarder>.run()
        super<AbstractMiddleComponent>.run()
    }
}

/**
 * Base abstract class for middleware nodes in the Spider branch.
 *
 * Extends [AbstractMiddleware] with [ItemForwarder] and [ItemAckForwarder] to also
 * intercept [Item] messages flowing toward the Pipeline branch and their [ItemAck]
 * backs returning toward the Spider.
 *
 * A [SpiderMiddleware] can therefore intercept and transform all message types
 * transiting through the Spider branch:
 * - Outgoing [AbstractRequest]s (via [RequestForwarder])
 * - Incoming [Response] backs (via [ResponseForwarder])
 * - Outgoing [Item]s (via [ItemForwarder])
 * - Incoming [ItemAck] backs (via [ItemAckForwarder])
 *
 * @param name The name of this middleware node.
 *
 * @see AbstractMiddleware
 * @see ItemForwarder
 * @see ItemAckForwarder
 */
abstract class SpiderMiddleware(
    name:String
):
    AbstractMiddleware(name),
    ItemForwarder,
    ItemAckForwarder
{

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        super<ItemForwarder>.run()
        super<ItemAckForwarder>.run()
        super<AbstractMiddleware>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping spider middleware"}
        super<AbstractMiddleware>.stop()
    }

}

/**
 * Base abstract class for middleware nodes in the Downloader branch.
 *
 * Extends [AbstractMiddleware] to intercept messages transiting through the
 * Downloader branch only:
 * - Outgoing [AbstractRequest]s (via [RequestForwarder])
 * - Incoming [Response] backs (via [ResponseForwarder])
 *
 * Unlike [SpiderMiddleware], it has no access to [Item] or [ItemAck] messages.
 *
 * @param name The name of this middleware node.
 *
 * @see AbstractMiddleware
 * @see SpiderMiddleware
 */
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
