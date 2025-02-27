package org.sbm4j.ktscraping.requests

import org.apache.hc.core5.net.URIBuilder
import org.sbm4j.ktscraping.core.RequestSender


open class AbstractInlineRequest(
    override val sender: RequestSender,
    url: String,
    params: Map<String, String>
): AbstractRequest(sender, url){
    init {
        var builder = URIBuilder(url)
        params.forEach{key, value -> builder = builder.addParameter(key, value)}
        this.url = builder.toString()
    }
}


