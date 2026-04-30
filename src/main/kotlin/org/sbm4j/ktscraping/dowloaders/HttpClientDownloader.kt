package org.sbm4j.ktscraping.dowloaders

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.network.*
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.meercat.data.Status
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel

/**
 * Defines how the HTTP response body should be read and stored in [DownloadingResponse.contents].
 *
 * Used internally by [HttpClientDownloader] to determine the appropriate reading strategy
 * based on the expected [ContentType] of the response.
 */
enum class BodyType{
    /** The response body is read as a [String] (HTML, JSON, XML, SVG, plain text). */
    TEXT,
    /** The response body is read as a [ByteArray] (bitmap or generic image). */
    IMAGE,
    /** The response body is read as a [ByteArray] (generic binary file). */
    FILE
}

/**
 * An [AbstractDownloader] that performs HTTP GET requests using the Ktor [HttpClient] (CIO engine).
 *
 * For each incoming [DownloadingRequest], opens a new [HttpClient], performs the GET request,
 * reads the response body according to the expected [ContentType] (via [getBodyType]), stores
 * the result in [DownloadingResponse.contents] under the [PAYLOAD] key, and closes the client.
 *
 * Error handling:
 * - [UnresolvedAddressException]: the URL could not be resolved — returns a [Status.FAIL] response.
 * - Any other exception: returns a [Status.ERROR] response.
 *
 * The expected [ContentType] is read from [AbstractRequest.parameters] under the [CONTENT_TYPE]
 * key. If not specified, the type is inferred from the URL (image extension → [BodyType.IMAGE],
 * otherwise [BodyType.TEXT]).
 *
 * @param name The name of this downloader node, defaults to `"HTTP Client downloader"`.
 *
 * @see AbstractDownloader
 * @see BodyType
 * @see DownloadingResponse
 */
class HttpClientDownloader(name: String = "HTTP Client downloader"): AbstractDownloader(name) {

    /**
     * Performs the HTTP GET request for the given [request] and returns a [DownloadingResponse]
     * populated with the downloaded content.
     *
     * Opens a new [HttpClient] for each request and closes it in the `finally` block.
     * The response body is read as text or bytes according to [getBodyType], and stored
     * in [DownloadingResponse.contents] under the [PAYLOAD] key.
     *
     * @param request The download request to process.
     * @return A [DownloadingResponse] with the downloaded content on success,
     * or an error response on failure.
     */
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
            val response = DownloadingResponse(request, ContentType.NOTHING, Status.FAIL, mutableListOf(infos))
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

    /**
     * Determines the [BodyType] for the given [request] based on its expected [ContentType].
     *
     * If [CONTENT_TYPE] is set in [AbstractRequest.parameters], maps it to the corresponding
     * [BodyType]. If not set, infers the type from the URL extension via [DownloadingRequest.isRawImage].
     * [ContentType.NOTHING] should not occur in practice and is reserved for error cases.
     *
     * @param request The request to determine the body type for.
     * @return The [BodyType] to use when reading the response body.
     */
    fun getBodyType(request: DownloadingRequest): BodyType{
        val expected = request.parameters.get(CONTENT_TYPE)
        return if(expected != null){
            when(expected as ContentType){
                ContentType.XML, ContentType.JSON, ContentType.SVG_IMAGE, ContentType.HTML -> BodyType.TEXT
                ContentType.BITMAP_IMAGE -> BodyType.IMAGE
                ContentType.IMAGE -> BodyType.IMAGE
                ContentType.FILE -> BodyType.FILE
                ContentType.NOTHING -> TODO()
                ContentType.STRING -> BodyType.TEXT
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

    /**
     * Determines the [ContentType] to set on the [DownloadingResponse] for the given [request].
     *
     * If [CONTENT_TYPE] is set in [AbstractRequest.parameters], it is used directly.
     * Otherwise, the type is inferred from the [bodyType]:
     * - [BodyType.TEXT] → [ContentType.HTML]
     * - [BodyType.IMAGE] → [ContentType.BITMAP_IMAGE]
     * - [BodyType.FILE] → [ContentType.FILE]
     *
     * @param request The request being processed.
     * @param bodyType The [BodyType] determined by [getBodyType].
     * @return The [ContentType] to set on the response.
     */
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