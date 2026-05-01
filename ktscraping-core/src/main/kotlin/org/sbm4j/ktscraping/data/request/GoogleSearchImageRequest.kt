package org.sbm4j.ktscraping.data.request

import org.apache.hc.core5.net.URIBuilder
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.middleware.ImageMiddleware
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

/**
 * A [DownloadingRequest] that encodes its parameters directly into the URL as a query string.
 *
 * Upon construction, the provided [params] are appended to [url] as inline query parameters
 * using [URIBuilder] (Apache HttpComponents). The resulting URL is stored back into [url],
 * ready to be used by the Downloader.
 *
 * This contrasts with other request styles where parameters may be passed as request body
 * or headers. Use this class when the target resource is identified by a URL with
 * query parameters (e.g. `https://example.com/search?q=kotlin&page=2`).
 *
 * @param sender The Spider emitting this request.
 * @param url The base URL of the resource to download.
 * @param params Query parameters to append to [url], defaults to an empty map.
 *
 * @see Request
 * @see DownloadingRequest
 */
open class AbstractInlineRequest(
    override var sender: SendSource,
    url: String,
    params: Map<String, String> = emptyMap()
): DownloadingRequest(sender, url){

    init {
        var builder = URIBuilder(url)
        params.forEach{key, value -> builder = builder.addParameter(key, value)}
        this.url = builder.toString()
    }

    /** Creates a copy of this request with the current (already-built) URL. */
    override fun clone(): DownloadingRequest {
        val result = AbstractInlineRequest(sender, url)
        return result
    }
}

/**
 * A request that queries the Google Custom Search API to find images matching a search text.
 *
 * Builds an [AbstractInlineRequest] targeting the Google Custom Search endpoint, with the
 * required API parameters (`key`, `cx`, `q`, `searchType=image`) inlined in the URL.
 *
 * The [parameters] map is populated with:
 * - [ImageMiddleware.Companion.JSON_PATH_IMAGES]: a map of JSONPath expressions to extract
 *   the title and thumbnail URL for each result image from the API response.
 * - [AbstractDownloader.Companion.CONTENT_TYPE]: set to [ContentType.JSON] to indicate
 *   the expected response format to the Downloader.
 *
 * The cache key is based on [researchText] only, so repeated searches for the same text
 * hit the cache regardless of other parameters.
 *
 * @param sender The Spider emitting this request.
 * @param researchText The search query text to look up images for.
 * @param key The Google Custom Search API key.
 * @param searchEngine The Custom Search Engine ID (`cx` parameter).
 * @param nbResults Number of image results to retrieve, defaults to `1`.
 *
 * @see AbstractInlineRequest
 * @see ImageMiddleware
 * @see AbstractDownloader
 */
class GoogleSearchImageRequest(
    sender: SendSource,
    val researchText: String,
    val key: String,
    val searchEngine: String,
    nbResults: Int = 1
): AbstractInlineRequest(sender, "https://www.googleapis.com/customsearch/v1",
    mapOf("key" to key,
        "cx" to searchEngine,
        "q" to researchText,
        "searchType" to "image")
    ){

    init {
        val paths = (0..nbResults - 1)
            .map{ "$.items[${it}].title" to "$.items[${it}].image.thumbnailLink" }
            .toMap()
        parameters[ImageMiddleware.Companion.JSON_PATH_IMAGES] = paths
        parameters[AbstractDownloader.Companion.CONTENT_TYPE] = ContentType.JSON
    }


    /**
     * Returns a cache key based on the search text only, so repeated searches
     * for the same query hit the cache regardless of API key or engine ID.
     */
    override fun toCacheKey(): String {
        return "google:${researchText}"
    }

}