package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.exporters.ItemDelete
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

interface DBControllable {

    var db: DBConnexion

    val name: String

    fun performDBItem(item: Item){
        when(item){
            is ItemUpdate -> {
                logger.debug { "${name}: update item ${item}" }
                performItemUpdate(item)
            }
            is ItemDelete -> {
                logger.debug { "${name}: delete item ${item}"}
                performItemDelete(item)
            }
            is DataItem<*> -> {
                logger.debug { "${name}: insert item ${item}"}
                perfomInsertItem(item)
            }
        }
    }

    fun performItemUpdate(item: ItemUpdate){
        db.performItemUpdate(item)
    }

    fun performItemDelete(item: ItemDelete){
        db.performItemDelete(item)
    }

    fun perfomInsertItem(item: DataItem<*>){
        db.perfomInsertItem(item)
    }
}