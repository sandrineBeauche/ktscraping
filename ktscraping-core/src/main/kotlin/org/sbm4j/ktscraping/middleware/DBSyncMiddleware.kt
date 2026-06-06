package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.processors.EventJobResult
import org.sbm4j.ktscraping.data.events.DBSyncEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.db.DBConnexion
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ItemDelete
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Response
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


enum class DBSyncState{
    UPTODATE,
    NEW
}

class SyncException(override val message: String) : Exception(message)

class DBSyncMiddleware<T: Data>(name: String): SpiderMiddleware(name) {
    companion object{
        val DBSYNC_STATE: String = "DBSyncState"
        val DBSYNC_KEY: String = "DBSyncKey"
        val DBSYNC: String = "DBSync"
    }

    var keys: Set<*>? = null

    val updatedKeys: MutableSet<Any> = mutableSetOf()

    lateinit var classObject: KClass<T>

    lateinit var keyProperty: KProperty1<T, *>

    lateinit var dbConnexion: DBConnexion

    var errorOccured: Boolean = false


    override suspend fun preStart(event: Event): EventJobResult? {
        return this.jobPreEvent(
            ErrorLevel.MAJOR,
            "cannot initialize key set from db connexion"
        ){
            if(keys == null) {
                keys = dbConnexion.getKeys(classObject, keyProperty)
            }
        }
    }




    override suspend fun processResponse(response: Response) {
        if(response is DownloadingResponse) {
            if (response.send.parameters.getOrDefault(DBSYNC_STATE, null) == DBSyncState.NEW) {
                response.contents[DBSYNC_STATE] = DBSyncState.NEW
            }
        }
    }


    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        val key = request.parameters.getOrDefault(DBSYNC_KEY, null)
        if(key != null){
            if(keys?.contains(key) == true){
                val result = DownloadingResponse(request)
                result.contents[DBSYNC_STATE] = DBSyncState.UPTODATE
                updatedKeys.add(key!!)
                return result
            }
            else{
                request.parameters[DBSYNC_STATE] = DBSyncState.NEW
            }
        }

        return request
    }

    override suspend fun preCustomEvent(event: Event): Any? {
        return when(event){
            is DBSyncEvent<*> -> preDBSyncEvent(event)
            else -> super.preCustomEvent(event)
        }
    }

    override suspend fun postCustomEvent(event: EventBack) {
        when(event.send){
            is DBSyncEvent<*> ->postDBSyncEvent(event)
            else -> super.postCustomEvent(event)
        }
    }


    suspend fun preDBSyncEvent(event: DBSyncEvent<*>): Any?{
        if(!errorOccured) {
            logger.debug { "${name}: get keys to delete to update database" }
            val keyToDelete = keys?.minus(updatedKeys)
            val itemDeletes = keyToDelete?.map {
                ItemDelete(classObject, keyProperty, it!!, sender = this)
            }!!
            val back = sendSyncAggregate(itemDeletes)
            if(back.status != Status.OK){
                this.pendingMinorError.getOrPut(event.channelableId){
                    mutableListOf<ErrorInfo>()
                }!!.addAll(back.errorInfos)
            }
            return null
        }
        else{
            throw SyncException("Could not sync database")
        }
    }

    fun postDBSyncEvent(event: EventBack){
        this.keys = dbConnexion.getKeys(classObject, keyProperty)
        this.updatedKeys.clear()
    }
}
