package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.DownloaderMiddleware
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response

class SchedulerMiddleware(scope: CoroutineScope, name: String = "Context middleware"): DownloaderMiddleware(scope, name) {


    override fun processResponse(response: Response): Boolean {
        TODO("Not yet implemented")
    }

    override fun processRequest(request: AbstractRequest): Any? {
        TODO("Not yet implemented")
    }
}