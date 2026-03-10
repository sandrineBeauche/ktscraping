package org.sbm4j.ktscraping.data.request

import org.apache.hc.core5.net.URIBuilder
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.middleware.ImageMiddleware
import org.sbm4j.meercat.components.SendSource

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

    override fun clone(): DownloadingRequest {
        val result = AbstractInlineRequest(sender, url)
        return result
    }
}

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


    override fun toCacheKey(): String {
        return "google:${researchText}"
    }

}