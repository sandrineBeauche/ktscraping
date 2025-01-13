package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import org.sbm4j.ktscraping.core.SpiderMiddleware
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.requests.*
import kotlin.reflect.KProperty


enum class DBSyncState{
    UPTODATE,
    NEW
}

class DBSyncMiddleware(scope: CoroutineScope, name: String): SpiderMiddleware(scope, name) {

    lateinit var keys: Set<Any>

    val updatedKeys: MutableSet<Any> = mutableSetOf()

    lateinit var classObject: Class<*>

    lateinit var keyProperty: KProperty<*>

    var errorOccured: Boolean = false

    override suspend fun processResponse(response: Response): Boolean {
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        println(request)
        if(request.parameters.containsKey("DBSyncKey")){
            val key = request.parameters["DBSyncKey"]
            if(keys.contains(key)){
                val result = Response(request, Status.OK)
                result.contents["DBSync"] = DBSyncState.UPTODATE
                updatedKeys.add(key!!)
                return result
            }
            else{
                request.parameters["DBSyncState"] = DBSyncState.NEW
            }
        }

        return request
    }

    override fun processItem(item: Item): List<Item> {
        when(item){
            is ItemError -> {
                errorOccured = true
                return listOf(item)
            }
            is ItemEnd -> return processItemEnd(item)
            else -> return listOf(item)
        }
    }

    fun processItemEnd(item: ItemEnd): List<Item>{
        val result: MutableList<Item> = mutableListOf()

        if(!errorOccured) {
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