package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.DownloaderMiddleware
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response

class CookiesMiddleware(name: String = "Cookies middleware") : DownloaderMiddleware(name) {
    companion object{
        val COOKIE_NAME: String = "cookiesName"
        val COOKIE: String = "cookies"
    }

    private val contexts: MutableMap<String, Any> = mutableMapOf()

    override suspend fun processRequest(request: AbstractRequest): Any? {
        val name = request.parameters[COOKIE_NAME] as String?
        if(name != null){
            val cook = contexts[name]
            if(cook != null){
                request.parameters[COOKIE] = cook
            }
        }
        return request
    }

    override suspend fun processResponse(response: Response): Boolean {
        val cook = response.contents.get(COOKIE)
        if(cook != null){
            val name = response.request.parameters.get(COOKIE_NAME) as String?
            if(name != null){
                contexts[name] = cook
            }
        }
        return true
    }


}