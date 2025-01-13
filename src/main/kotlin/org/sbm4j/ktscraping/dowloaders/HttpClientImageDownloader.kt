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

    val rawExtensions: List<String> = listOf("png", "bmp", "jpg", "jpeg")


    override suspend fun processRequest(request: AbstractRequest): Any? {

        val client = HttpClient(CIO)

        try{
            val response = Response(request)
            val extension = request.url.split(".").last()

            val resp: HttpResponse = client.get(request.url)
            if(rawExtensions.contains(extension)) {
                response.contents["imagePayload"] = resp.readRawBytes()
            }
            else{
                response.contents["payload"] = resp.bodyAsText()
            }
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