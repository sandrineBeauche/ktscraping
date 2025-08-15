package org.sbm4j.ktscraping.middleware

import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sbm4j.ktscraping.core.AbstractDownloader
import org.sbm4j.ktscraping.core.ContentType
import org.sbm4j.ktscraping.core.DownloaderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.Status
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

enum class CacheAvailability{
    UNDEFINED {
        override fun duration(): Long {
            return Duration.ZERO.inWholeSeconds
        }
    },
    NEVER {
        override fun duration(): Long {
            return Duration.ZERO.inWholeSeconds
        }
    },
    EXECUTION {
        override fun duration(): Long {
            return Duration.ZERO.inWholeSeconds
        }
    },
    HOUR {
        override fun duration(): Long {
            return 1.hours.inWholeSeconds
        }
    },
    DAY {
        override fun duration(): Long {
            return 1.days.inWholeSeconds
        }
    },
    WEEK {
        override fun duration(): Long {
            return 7.days.inWholeSeconds
        }
    },
    MONTH {
        override fun duration(): Long {
            return 30.days.inWholeSeconds
        }
    },
    YEAR {
        override fun duration(): Long {
            return 365.days.inWholeSeconds
        }
    },
    ALWAYS {
        override fun duration(): Long {
            return Duration.INFINITE.inWholeSeconds
        }
    };

    abstract fun duration(): Long
}

val jsonCacheMiddleware = Json {
    prettyPrint = true
}

@Serializable
data class CacheEntry(
    val key: String = UUID.randomUUID().toString(),
    val availability: CacheAvailability = CacheAvailability.UNDEFINED,
    val timestamp: Long = now().epochSeconds,
    val contentType: ContentType = ContentType.HTML,
    val payload: String = "",
    val frames: Map<String, String> = mapOf(),
){
    fun isOutOfDate(): Boolean{
        return when(availability){
            CacheAvailability.UNDEFINED, CacheAvailability.NEVER, CacheAvailability.EXECUTION -> true
            CacheAvailability.ALWAYS -> false
            else -> {
                val duration = availability.duration()
                val now = now().epochSeconds
                (now - timestamp) > duration
            }
        }
    }
}

class CacheMiddleware(name: String = "Cache middleware"): DownloaderMiddleware(name) {
    companion object{
        val CACHE_AVAILABILITY: String = "CACHE_AVAILABILITY"
        val CACHE_KEY: String = "CACHE_KEY"
    }


    var availability: CacheAvailability = CacheAvailability.ALWAYS

    val cacheMap: MutableMap<String, CacheEntry> = mutableMapOf()

    val cacheFilename: String = "cache.json"

    lateinit var root: File


    override suspend fun processDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        if(response.status == Status.OK) {
            val avail = request.parameters[CACHE_AVAILABILITY] as CacheAvailability
            if (avail != CacheAvailability.NEVER) {
                val cacheKey = request.parameters.getOrDefault(CACHE_KEY, request.toCacheKey()) as String
                val timestamp = now().epochSeconds
                val contentType = response.type

                val fileId = UUID.randomUUID()

                try {
                    val payload = response.contents[AbstractDownloader.PAYLOAD] as String
                    val payloadFile = File(root, "${fileId}")
                    payloadFile.writeText(payload)

                    val framesMap = response.contents[AbstractDownloader.FRAMES]
                    val frames = mutableMapOf<String, String>()
                    if (framesMap != null && (framesMap as Map<*, *>).isNotEmpty()) {
                        val f = framesMap.mapValues { (key, value) ->
                            val filename = "${fileId}_${key}"
                            val fileToWrite = File(root, filename)
                            fileToWrite.writeText(value as String)
                            filename
                        }
                        frames.putAll(f as Map<String, String>)
                    }
                    val entry = CacheEntry(cacheKey, avail, timestamp, contentType, "${fileId}", frames)
                    cacheMap[cacheKey] = entry
                }
                catch(ex: Exception){
                    logger.error{"${name}: error when saving payload or frames for key ${cacheKey}. Response is not cached."}
                }
            }
        }

        return true
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        val cacheKey = request.parameters.getOrDefault(CACHE_KEY, request.toCacheKey()) as String

        val cacheValue = cacheMap[cacheKey]
        if(cacheValue == null){
            val reqCacheAvail = request.parameters[CACHE_AVAILABILITY]
            if(reqCacheAvail == null ||
                reqCacheAvail == CacheAvailability.UNDEFINED ||
                (reqCacheAvail as CacheAvailability) > availability){
                request.parameters[CACHE_AVAILABILITY] = availability
            }
            return request
        }
        else{
            try {
                val entry = cacheValue
                val response = DownloadingResponse(request, entry.contentType)
                val payload = File(root, entry.payload).readText()
                response.contents[AbstractDownloader.PAYLOAD] = payload

                val frames = entry.frames.mapValues { (_, value) ->
                    File(root, value).readText()
                }
                if (frames.isNotEmpty()) {
                    response.contents[AbstractDownloader.FRAMES] = frames
                }
                return response
            }
            catch(ex: Exception){
                logger.error { "${name}: Error when retrieving cache entry for key $cacheKey. Removing key..." }
                cacheMap.remove(cacheKey)
                return request
            }
        }

    }

    override suspend fun run() {
        loadCache(cacheFilename)
        super.run()
    }

    fun loadCache(jsonFile: File){
        try {
            val jsonText = jsonFile.readText()
            val entries = jsonCacheMiddleware.decodeFromString<List<CacheEntry>>(jsonText)
                .filter { !it.isOutOfDate() }
                .associateBy { it.key }
            cacheMap.putAll(entries)
        } catch (ex: Exception) {
            logger.error { "${name}: Error when retrieving entries cache from ${jsonFile.absolutePath}. Cache is empty now" }
        }
    }

    fun loadCache(filename: String){
        val jsonFile = File(root, filename)
        if(jsonFile.exists()) {
            logger.info { "${name}: retrieve cache from file ${jsonFile.absolutePath}" }
            loadCache(jsonFile)
        }
    }

    fun saveCache(filename: String){
        val jsonFile = File(root, filename)
        try {
            logger.info { "${name}: save cache to the file ${jsonFile.absolutePath}" }
            val entries = cacheMap.filter { (_, value) ->
                value.availability != CacheAvailability.UNDEFINED &&
                value.availability != CacheAvailability.NEVER &&
                value.availability != CacheAvailability.EXECUTION
            }.values.toList()
            val json = jsonCacheMiddleware.encodeToString(entries)
            jsonFile.writeText(json)
        }
        catch(ex: Exception){
            logger.error{"${name}: Error when saving cache to ${jsonFile.absolutePath}. File is deleted"}
            if(jsonFile.exists()){
                jsonFile.delete()
            }
        }
    }

    override suspend fun stop() {
        saveCache(cacheFilename)
        super.stop()
    }
}