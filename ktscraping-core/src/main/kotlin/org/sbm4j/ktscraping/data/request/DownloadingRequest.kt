package org.sbm4j.ktscraping.data.request

import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

/**
 * Base class for HTTP-like download requests transiting through the Downloader branch.
 *
 * Extends [AbstractRequest] with a [url] property and utility methods for URL-based
 * downloading. The [url] is used both as the resource identifier ([toURIString]) and
 * as the barrier key for synchronization in [Barrier] nodes.
 *
 * @param sender The Spider emitting this request.
 * @param url The URL of the resource to download.
 *
 * @see Request
 * @see DownloadingResponse
 */
abstract class DownloadingRequest(
    sender: SendSource,
    open var url: String
): AbstractRequest(sender){

    companion object{
        /**
         * File extensions considered as raw images.
         * Used by [isRawImage] to detect image resources from their URL.
         */
        val rawExtensions: List<String> = listOf("png", "bmp", "jpg", "jpeg")
    }

    /**
     * Extracts the server hostname from [url].
     *
     * For example, `"https://example.com/path"` returns `"example.com"`.
     *
     * @return The server hostname, or the full string after `"://"` if no path is present.
     */
    fun extractServerFromUrl(): String{
        val start = url.indexOf("://")
        val end = url.indexOf("/", start + 3)
        return if(end > 0){
            url.substring(start + 3, end)
        } else{
            url.substring(start + 3)
        }
    }

    /**
     * Returns `true` if the URL points to a raw image resource,
     * determined by its file extension against [rawExtensions].
     */
    fun isRawImage(): Boolean{
        val extension = url.split(".").last()
        return rawExtensions.contains(extension)
    }

    /**
     * Returns the cache key for this request.
     *
     * Defaults to `"url:<url>"`. Subclasses may override this to include
     * additional parameters in the cache key.
     *
     * @return A string uniquely identifying this request for caching purposes.
     */
    open fun toCacheKey(): String {
        return "url:${url}"
    }

    /**
     * Builds an error back with no content ([ContentType.NOTHING]).
     *
     * @param infos Details of the error that occurred.
     * @param status Error status to apply.
     * @return A [DownloadingResponse] with the provided error info and no content.
     */
    override fun buildErrorBack(infos: ErrorInfo, status: Status): DownloadingResponse {
        return DownloadingResponse(this, ContentType.NOTHING,
            status, mutableListOf(infos), "${this.name}-Response")
    }

    /**
     * Builds a nominal back with no content ([ContentType.NOTHING]).
     * The Downloader will populate the response content upon processing.
     *
     * @return A [DownloadingResponse] with [Status.OK] and no content yet.
     */
    override fun buildBack(): DownloadingResponse {
        return DownloadingResponse(this, ContentType.NOTHING, name = "${this.name}-Response")
    }

    /** Creates a copy of this request. Must be implemented by each subclass. */
    abstract override fun clone(): DownloadingRequest

    /**
     * Returns [url] as the barrier key, allowing [Barrier] nodes to synchronize
     * branches on the requested URL.
     */
    override fun getKeyBarrier(): String {
        return this.url
    }

    /**
     * Returns [url] as the URI string representation of this request.
     */
    override fun toURIString(): String {
        return this.url
    }
}

/**
 * Default concrete implementation of [DownloadingRequest].
 *
 * A simple URL-based download request with no additional parameters.
 * Use this class for straightforward downloads where no custom logic is needed.
 *
 * @property sender The Spider emitting this request.
 * @property url The URL of the resource to download.
 */
data class Request(
    override var sender: SendSource,
    override var url: String
): DownloadingRequest(sender, url){

    override fun clone(): Request {
        return this.copy()
    }
}

