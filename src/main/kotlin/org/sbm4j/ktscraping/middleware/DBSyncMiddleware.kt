package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.requests.*
import kotlin.reflect.KProperty


enum class DBSyncState{
    UPTODATE,
    NEW
}

class DBSyncMiddleware(name: String): SpiderMiddleware(name) {
    companion object{
        val DBSYNC_STATE: String = "DBSyncState"
        val DBSYNC_KEY: String = "DBSyncKey"
        val DBSYNC: String = "DBSync"
    }

    lateinit var keys: Set<Any>

    val updatedKeys: MutableSet<Any> = mutableSetOf()

    lateinit var classObject: Class<*>

    lateinit var keyProperty: KProperty<*>

    var errorOccured: Boolean = false

    override suspend fun processResponse(response: Response): Boolean {
        if(response.request.parameters.getOrDefault(DBSYNC_STATE, null) == DBSyncState.NEW){
            response.contents[DBSYNC_STATE] = DBSyncState.NEW
        }
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        if(request.parameters.containsKey(DBSYNC_KEY)){
            val key = request.parameters[DBSYNC_KEY]
            if(keys.contains(key)){
                val result = Response(request, Status.OK)
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

    override suspend fun processItem(item: Item): List<Item> {
        when(item){
            is ItemError -> {
                if(item.level == ErrorLevel.MAJOR || item.level == ErrorLevel.FATAL) {
                    errorOccured = true
                }
                return listOf(item)
            }
            is ItemEnd -> return processItemEnd(item)
            else -> return listOf(item)
        }
    }

    fun processItemEnd(item: ItemEnd): List<Item>{
        val result: MutableList<Item> = mutableListOf()

        if(!errorOccured) {
            logger.debug { "${name}: get keys to delete to update database" }
            val keyToDelete = keys - updatedKeys
            val itemDeletes = keyToDelete.map {
                ItemDelete(classObject, keyProperty, it)
            }
            result.addAll(itemDeletes)
        }

        result.addLast(item)
        return result
    }
}