package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractMiddleware
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

class ContextMiddleware(scope: CoroutineScope, name: String) : AbstractMiddleware(scope, name) {

    val contexts: MutableMap<RequestSender, Any> = mutableMapOf()

    override fun processRequest(request: Request): Any? {
        if(contexts.containsKey(request.sender)){
            request.parameters["context"] = contexts.containsKey(request.sender)
        }
        return request
    }

    override fun processResponse(response: Response): Boolean {
        if(response.contents.containsKey("context")){
            contexts[response.request.sender] = response.contents["context"]!!
        }
        return true
    }


}