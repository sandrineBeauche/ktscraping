package org.sbm4j.ktscraping.dowloaders

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.network.*
import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response
import org.sbm4j.ktscraping.requests.Status

enum class BodyType{
    TEXT,
    IMAGE,
    FILE
}

class HttpClientDownloader(name: String = "HTTP Client downloader"): AbstractDownloader(name) {


    override suspend fun processRequest(request: AbstractRequest): Any? {

        val client = HttpClient(CIO)

        try{
            val response = Response(request)
            val resp: HttpResponse = client.get(request.url)
            val bodyType = getBodyType(request)
            when(bodyType){
                BodyType.TEXT -> response.contents[PAYLOAD] = resp.bodyAsText()
                BodyType.IMAGE -> response.contents[PAYLOAD] = resp.readRawBytes()
                BodyType.FILE -> response.contents[PAYLOAD] = resp.readRawBytes()
            }

            response.type = getResponseType(request, bodyType)
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

    fun getBodyType(request: AbstractRequest): BodyType{
        val expected = request.parameters.get(CONTENT_TYPE)
        return if(expected != null){
            when(expected as ContentType){
                ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> BodyType.TEXT
                ContentType.BITMAP_IMAGE -> BodyType.IMAGE
                ContentType.IMAGE -> BodyType.IMAGE
                ContentType.FILE -> BodyType.FILE
            }
        }
        else{
            if(request.isRawImage()){
                BodyType.IMAGE
            }
            else{
                BodyType.TEXT
            }
        }
    }

    fun getResponseType(request: AbstractRequest, bodyType: BodyType): ContentType{
        val expectedType = request.parameters.get(CONTENT_TYPE)
        return if(expectedType == null){
            when(bodyType){
                BodyType.TEXT -> ContentType.HTML
                BodyType.IMAGE -> ContentType.BITMAP_IMAGE
                BodyType.FILE -> ContentType.FILE
            }
        }
        else{
            expectedType as ContentType
        }
    }
}