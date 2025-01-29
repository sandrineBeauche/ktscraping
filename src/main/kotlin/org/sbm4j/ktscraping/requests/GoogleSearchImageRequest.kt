package org.sbm4j.ktscraping.requests

import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.middleware.ImageMiddleware

class GoogleSearchImageRequest(
    sender: RequestSender,
    researchText: String,
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
        parameters[ImageMiddleware.JSON_PATH_IMAGES] = paths
        parameters[AbstractDownloader.CONTENT_TYPE] = ContentType.JSON
    }

}
