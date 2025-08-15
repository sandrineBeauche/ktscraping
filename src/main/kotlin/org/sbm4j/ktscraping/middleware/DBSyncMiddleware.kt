package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.sbm4j.ktscraping.core.EventJobResult
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.Event
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ErrorLevel
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.item.EndItem
import org.sbm4j.ktscraping.data.item.ErrorItem
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.reflect.KProperty1


enum class DBSyncState{
    UPTODATE,
    NEW
}

class DBSyncMiddleware<T: Data>(name: String): SpiderMiddleware(name) {
    companion object{
        val DBSYNC_STATE: String = "DBSyncState"
        val DBSYNC_KEY: String = "DBSyncKey"
        val DBSYNC: String = "DBSync"
    }

    var keys: Set<*>? = null

    val updatedKeys: MutableSet<Any> = mutableSetOf()

    lateinit var classObject: Class<T>

    lateinit var keyProperty: KProperty1<T, *>

    lateinit var dbConnexion: DBConnexion

    var errorOccured: Boolean = false


    override suspend fun preStart(event: Event): EventJobResult? {
        if(keys == null) {
            keys = dbConnexion.getKeys(classObject, keyProperty)
        }
        return super.preStart(event)
    }

    override suspend fun processDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        if(response.request.parameters.getOrDefault(DBSYNC_STATE, null) == DBSyncState.NEW){
            response.contents[DBSYNC_STATE] = DBSyncState.NEW
        }
        return true
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        if(request.parameters.containsKey(DBSYNC_KEY)){
            val key = request.parameters[DBSYNC_KEY]
            if(keys?.contains(key) == true){
                val result = DownloadingResponse(request)
                result.contents[DBSYNC] = DBSyncState.UPTODATE
                updatedKeys.add(key!!)
                return result
            }
            else{
                request.parameters[DBSYNC_STATE] = DBSyncState.NEW
            }
        }

        return request
    }


    override suspend fun preEnd(event: Event): EventJobResult? {
        val result: MutableList<Item> = mutableListOf()

        if(!errorOccured) {
            logger.debug { "${name}: get keys to delete to update database" }
            val keyToDelete = keys?.minus(updatedKeys)
            val itemDeletes = keyToDelete?.map {
                ItemDelete(classObject, keyProperty, classObject.cast(it))
            }!!
            result.addAll(itemDeletes)
        }


        return super.preEnd(event)
    }

}