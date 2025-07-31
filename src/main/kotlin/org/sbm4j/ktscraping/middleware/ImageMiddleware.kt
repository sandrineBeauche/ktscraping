package org.sbm4j.ktscraping.middleware

import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import it.skrape.core.htmlDocument
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.ResponseException
import java.io.File

class ImageDescriptor(val name: String){

    var rawStringData: String? = null

    var rawBytesData: ByteArray? = null

    var dataFile: File? = null
}


class ImageMiddleware(name: String): SpiderMiddleware(name) {

    companion object{
        val CSS_SELECTOR_IMAGES: String = "cssSelectorImages"
        val JSON_PATH_IMAGES: String = "JsonPathImages"
        val IMAGES: String = "images"
        val IMAGES_PAYLOAD: String = "imagesPayload"
        val IMAGES_ROOT: String = "imagesRoot"
    }

    override suspend fun processResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        if(response.status == Status.OK &&
            (response.request.parameters.contains(CSS_SELECTOR_IMAGES) ||
                    response.request.parameters.containsKey(JSON_PATH_IMAGES))){

            val payload = getPayload(response)
            var images: Map<String, String> = mapOf()
            if (response.request.parameters.contains(CSS_SELECTOR_IMAGES)) {
                val cssSelectors = (response.request.parameters[CSS_SELECTOR_IMAGES] as Map<*, *>)
                images = searchImageCSS(cssSelectors, payload)
            }
            if(response.request.parameters.containsKey(JSON_PATH_IMAGES)){
                val jsonPaths = (response.request.parameters[JSON_PATH_IMAGES] as Map<String, String>)
                images = searchImageJsonPath(jsonPaths, payload)
            }
            response.contents[IMAGES] = images

            val imagesPayload = images.map {
                if(it.value.startsWith("data:image")){
                    val id = ImageDescriptor(it.key)
                    id.rawStringData = it.value
                    it.key to id
                }
                else {
                    val imageValue = downloadImage(it.key, it.value, response.request)
                    it.value to imageValue
                }
            }.toMap()

            response.contents[IMAGES_PAYLOAD] = imagesPayload
        }
        return true
    }

    fun getPayload(response: DownloadingResponse): String{
        return when(response.type){
            ContentType.XML, ContentType.HTML, ContentType.JSON -> {
                val payload = response.contents[AbstractDownloader.PAYLOAD]
                if(payload == null) {
                    throw ResponseException("payload for response on request ${response.request.name} is null")
                }
                else payload as String
            }
            else -> throw ResponseException("try to extract images on a non-string payload")
        }
    }


    fun searchImageCSS(cssSelectors: Map<*, *>, payload: String): Map<String, String>{
        val images = cssSelectors.map {
            htmlDocument(payload) {
                findAll("${it.key} img"){
                    val nbElt = it.value as Int
                    val elts = if(nbElt == 0) {
                        this
                    }
                    else{
                        take(it.value as Int)
                    }
                    elts.map { current ->
                        current.eachImage
                    }.reduce { acc, map -> acc + map }
                }
            }
        }.reduce { acc, map -> acc + map }
        return images
    }


    fun searchImageJsonPath(jsonPaths: Map<String ,String>, payload: String): Map<String, String>{
        try {
            val node = JsonPath.parse(payload)
            val results = jsonPaths.map {
                val title = node?.read<String>(it.key)
                val link = node?.read<String>(it.value)
                title!! to link!!
            }.toMap()
            return results
        }
        catch(ex: Exception){
            return mapOf()
        }
    }

    suspend fun downloadImage(name:String, link: String, request: DownloadingRequest): ImageDescriptor? {
        val req = Request(this@ImageMiddleware, link)
        req.parameters[AbstractDownloader.CONTENT_TYPE] = ContentType.IMAGE
        val response = this.sendSync(req) as DownloadingResponse
        if(response.status == Status.OK) {
            val result = ImageDescriptor(name)
            val payload: Any = response.contents[AbstractDownloader.PAYLOAD]!!
            if (request.parameters.containsKey(IMAGES_ROOT)) {
                val f = getImageFile(request, link)
                if (response.isByteArray()) {
                    f.writeBytes(payload as ByteArray)
                } else {
                    f.writeText(payload as String)
                }
                result.dataFile = f
            } else {
                if(response.isText()) {
                    result.rawStringData = payload as String?
                }
                else {
                    result.rawBytesData = payload as ByteArray?
                }
            }
            return result
        }
        else{
            return null
        }
    }

    fun getImageFile(request: AbstractRequest, link: String): File{
        val root = request.parameters[IMAGES_ROOT] as String
        val filename = link.split("/").last()
        return File(root, filename)
    }




    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }

    override suspend fun processDataItem(item: ObjectDataItem<*>): List<Item> {
        return listOf(item)
    }
}