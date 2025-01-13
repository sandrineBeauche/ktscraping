package org.sbm4j.ktscraping.middleware

import it.skrape.core.htmlDocument
import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.requests.*
import java.io.File

class ImageDescriptor(val name: String){

    var rawStringData: String? = null

    var rawBytesData: ByteArray? = null

    var dataFile: File? = null
}


class ImageMiddleware(scope: CoroutineScope, name: String): SpiderMiddleware(scope, name) {

    override suspend fun processResponse(response: Response): Boolean {
        if(response.request.parameters.contains("cssSelectorImages")) {
            val cssSelectors = (response.request.parameters["cssSelectorImages"] as Map<*, *>)
            val payload = response.contents["payload"] as String
            val images = searchImage(cssSelectors, payload)

            response.contents["images"] = images

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

            response.contents["imagesPayload"] = imagesPayload
        }
        return true
    }

    fun searchImage(cssSelectors: Map<*, *>, payload: String): Map<String, String>{
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

    suspend fun downloadImage(name:String, link: String, request: AbstractRequest): ImageDescriptor? {
        val req = Request(this@ImageMiddleware, link)
        val response = this.sendSync(req)
        if(response.status == Status.OK) {
            val result = ImageDescriptor(name)
            if (request.parameters.containsKey("imagesRoot")) {
                val root = request.parameters["imagesRoot"] as String
                val filename = link.split("/").last()
                val f = File(root, filename)
                if (response.contents.containsKey("imagePayload")) {
                    val bytes = response.contents["imagePayload"] as ByteArray
                    f.writeBytes(bytes)
                } else {
                    val text = response.contents["payload"] as String
                    f.writeText(text)
                }
                result.dataFile = f
            } else {
                result.rawStringData = response.contents.getOrDefault("payload", null) as String?
                result.rawBytesData = response.contents.getOrDefault("imagePayload", null) as ByteArray?
            }
            return result
        }
        else{
            return null
        }
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        return true
    }

    override fun processItem(item: Item): Item? {
        return item
    }
}