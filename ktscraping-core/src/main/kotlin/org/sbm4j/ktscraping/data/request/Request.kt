package org.sbm4j.ktscraping.data.request

import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for request messages transiting through the Downloader branch.
 *
 * A request represents a download task emitted by a Spider and processed by the
 * Downloader branch. KtScraping is not limited to HTTP — any download source
 * (HTTP, FTP, local files, databases, etc.) can be supported by providing
 * a concrete implementation of this class.
 *
 * The [parameters] map is a generic key-value store that can hold any data
 * relevant to the request (e.g. query parameters, headers, authentication tokens,
 * downloader-specific configuration, etc.).
 *
 * Subclasses must implement [toURIString] to provide a source-specific
 * string representation of the resource to download.
 *
 * @param sender The Spider emitting this request.
 *
 * @see toURIString
 */
abstract class AbstractRequest(override var sender: SendSource): Send {
    companion object {
        /** Global atomic counter used to generate unique request names. */
        val lastId = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    /** Technical name of this request, auto-generated as `"Request-<id>"`. */
    override val name = "Request-${lastId.getAndIncrement()}"

    /**
     * Generic parameter map for this request.
     *
     * Can hold any data relevant to the download (e.g. query parameters, headers,
     * authentication tokens, downloader-specific options, etc.).
     * Not restricted to HTTP — any download source may use this map freely.
     */
    val parameters: MutableMap<String, Any> = mutableMapOf()

    /**
     * Returns a string representation of the resource to download.
     *
     * The format is source-specific: an HTTP URL, a file path, a database query, etc.
     * Subclasses must provide a meaningful implementation for their target source.
     *
     * @return A string identifying the resource to download.
     */
    abstract fun toURIString(): String

}