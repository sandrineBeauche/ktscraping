package org.sbm4j.ktscraping.dowloaders

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status

class HttpClientImageDownloader(scope: CoroutineScope, name: String): AbstractDownloader(scope, name) {

    override suspend fun processRequest(request: AbstractRequest): Any? {
        lateinit var bytes: ByteArray

        val client = HttpClient(CIO)

        try{
            val resp: HttpResponse = client.get(request.url)
            bytes = resp.readBytes()

            val response = Response(request)
            response.contents["imagePayload"] = bytes
            return response
        }
        catch(ex: UnresolvedAddressException){
            val response = Response(request, Status.NOT_FOUND)
            response.contents["error"] = "Address ${request.url} not found for request ${request.name}"
            return response
        }
        catch(ex: Exception){
            val response = Response(request, Status.ERROR)
            response.contents["error"] = ex
            return response
        }
        finally {
            client.close()
        }




    }
}