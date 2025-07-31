package org.sbm4j.ktscraping.dowloaders

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.network.*
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse

enum class BodyType{
    TEXT,
    IMAGE,
    FILE
}

class HttpClientDownloader(name: String = "HTTP Client downloader"): AbstractDownloader(name) {


    override suspend fun processDataRequest(request: DownloadingRequest): Any? {

        val client = HttpClient(CIO)

        try{
            val response = DownloadingResponse(request)
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
            val message = "Address ${request.url} not found for request ${request.name}"
            val infos = ErrorInfo(ex, this, ErrorLevel.MAJOR, message)
            val response = DownloadingResponse(request, ContentType.NOTHING,
                Status.NOT_FOUND, mutableListOf(infos))
            return response
        }
        catch(ex: Exception){
            val infos = ErrorInfo(ex, this, ErrorLevel.MAJOR)
            val response = DownloadingResponse(request, ContentType.NOTHING,
                Status.ERROR, mutableListOf(infos))
            return response
        }
        finally {
            client.close()
        }
    }

    fun getBodyType(request: DownloadingRequest): BodyType{
        val expected = request.parameters.get(CONTENT_TYPE)
        return if(expected != null){
            when(expected as ContentType){
                ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> BodyType.TEXT
                ContentType.BITMAP_IMAGE -> BodyType.IMAGE
                ContentType.IMAGE -> BodyType.IMAGE
                ContentType.FILE -> BodyType.FILE
                ContentType.NOTHING -> TODO()
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

    fun getResponseType(request: DownloadingRequest, bodyType: BodyType): ContentType{
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