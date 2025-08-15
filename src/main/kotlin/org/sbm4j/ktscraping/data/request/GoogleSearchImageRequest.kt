package org.sbm4j.ktscraping.data.request

import org.apache.hc.core5.net.URIBuilder
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.middleware.ImageMiddleware

open class AbstractInlineRequest(
    override var sender: Controllable,
    url: String,
    params: Map<String, String>
): DownloadingRequest(sender, url){
    init {
        var builder = URIBuilder(url)
        params.forEach{key, value -> builder = builder.addParameter(key, value)}
        this.url = builder.toString()
    }
}

class GoogleSearchImageRequest(
    sender: RequestSender,
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