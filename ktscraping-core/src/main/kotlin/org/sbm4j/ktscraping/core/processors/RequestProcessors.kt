package org.sbm4j.ktscraping.core.processors

import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder
import org.sbm4j.meercat.nodes.sendProcessors.SendSource




class NoRequestSenderException(message: String) : Exception(message)



interface  RequestSender: SendSource {


}

/**
 * A [BackForwarder] that processes [Response] messages returning from the Downloader branch.
 *
 * When a [Response] is received from the Downloader, [processResponse] is called to handle
 * it — typically to parse the downloaded content, extract [Item]s, or trigger new [Request]s
 * before forwarding the back upstream toward the Spider.
 *
 * @see Response
 */
interface ResponseForwarder: BackForwarder {

    /**
     * Processes a [Response] received from the Downloader branch.
     *
     * Implement this method to handle the downloaded content: parse HTML, extract data,
     * emit new [Request]s or [Item]s, etc.
     *
     * @param response The response to process.
     */
    suspend fun processResponse(response: Response)

    override suspend fun run() {
        val flow = this.outChannel.getBackFlow<Response>(Response::class, this)
        receiveBacks(Response::class, flow, ::processResponse)
    }
}

/**
 * A [SendConsumer] that receives [AbstractRequest] messages in the Downloader branch.
 *
 * Entry point of the Downloader branch, [RequestReceiver] dispatches incoming requests
 * to the appropriate processing hook based on their type. Concrete implementations
 * override [processDataRequest] to perform the actual download and populate the
 * [Response] back with the retrieved content.
 *
 * @see AbstractRequest
 * @see DownloadingRequest
 * @see Response
 */
interface RequestReceiver: SendConsumer {

    /**
     * Dispatches the incoming [request] to the appropriate processing hook.
     *
     * Routes [DownloadingRequest] to [processDataRequest]. Other request types
     * are acknowledged without processing.
     *
     * @param request The incoming request to process.
     * @return The result of the processing hook, or `true` if no hook matched.
     */
    suspend fun processRequest(request: AbstractRequest): Any?{
        return when(request){
            is DownloadingRequest -> processDataRequest(request)
            else -> true
        }
    }

    /**
     * Hook called when a [DownloadingRequest] is received.
     *
     * Override to perform the actual download for the given request and return
     * a [Response] populated with the retrieved content. Defaults to `null`.
     *
     * @param request The download request to process.
     * @return A [Response] carrying the downloaded content, or `null` if not handled.
     */
    suspend fun processDataRequest(request: DownloadingRequest): Any?{
        return null
    }

    override suspend fun run() {
        logger.debug{"run from Request processor"}
        val clazz = AbstractRequest::class
        val flow = inChannel.getSendFlow(clazz)
        this.performSends(clazz, flow, ::processRequest)
        logger.debug{"done run from Request processor"}
    }
}

/**
 * A node that both receives [AbstractRequest] messages and forwards them downstream
 * in the Downloader branch.
 *
 * Combines [RequestReceiver] and [SendForwarder] to act as a middleware in the Downloader
 * branch: it can intercept and process incoming requests (e.g. enrich parameters, apply
 * rate limiting, check cache) before forwarding them to the next node.
 *
 * Both [RequestReceiver.run] and [SendForwarder.run] are executed concurrently on startup.
 *
 * @see RequestReceiver
 * @see SendForwarder
 */
interface RequestForwarder: RequestReceiver, SendForwarder {
    override suspend fun run() {
        super<RequestReceiver>.run()
        super<SendForwarder>.run()
    }
}
