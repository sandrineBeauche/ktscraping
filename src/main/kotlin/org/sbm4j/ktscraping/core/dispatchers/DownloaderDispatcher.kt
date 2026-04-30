package org.sbm4j.ktscraping.core.dispatchers

import org.kodein.di.DI
import org.sbm4j.ktscraping.core.components.AbstractComponent
import org.sbm4j.ktscraping.core.components.State
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.dispatchers.AbstractPropagator
import org.sbm4j.meercat.nodes.dispatchers.Router
import org.sbm4j.meercat.nodes.logger

/**
 * Base abstract class for dispatching messages in the Downloader branch.
 *
 * [DownloaderDispatcher] serves a dual role in the topology:
 * - As a [Router]: routes each incoming [AbstractRequest] to the appropriate Downloader
 *   via [selectChannel], and forwards the corresponding [Response] backs toward the Spider.
 * - As an [EventDispatcher]: broadcasts [Event] messages to all registered Downloader
 *   branches simultaneously.
 *
 * This allows a topology with multiple Downloaders (e.g. an HTTP downloader, a Playwright
 * downloader, a file downloader) to coexist, each receiving only the requests it handles
 * and all receiving lifecycle events.
 *
 * Concrete subclasses implement [selectChannel] to define the routing logic based on
 * the request type or content (e.g. routing [PlaywrightRequest] to the Playwright downloader).
 *
 * @param name The name of this dispatcher node, defaults to `"DownloaderDispatcher"`.
 * @param di The Kodein dependency injection container, used for topology construction.
 *
 * @see Router
 * @see EventDispatcher
 * @see AbstractPropagator
 */
abstract class DownloaderDispatcher(
    override val name: String = "DownloaderDispatcher",
    override val di: DI
): EventDispatcher, Router, AbstractPropagator() {

    override var state: State = State()

    /**
     * Selects the output [SuperChannel] to route the given [request] to.
     *
     * Implement this method to define the routing logic between multiple Downloaders
     * (e.g. based on request type, URL pattern, or any other criterion).
     *
     * @param request The incoming request to route.
     * @return The [SuperChannel] of the target Downloader for this request.
     */
    abstract fun selectChannel(request: AbstractRequest): SuperChannel


    override suspend fun run() {
        logger.info{ "${name}: Starting the downloader dispatcher"}
        performSendBacks(AbstractRequest::class, Response::class,
            null, ::selectChannel)
        super<EventDispatcher>.run()
        super<AbstractPropagator>.run()
    }

    override suspend fun stop() {
        logger.info{ "${name}: Stopping the downloader dispatcher"}
        super<AbstractPropagator>.stop()
    }
}