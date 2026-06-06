package org.sbm4j.ktscraping.data.events

import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.reflect.KClass

data class DBSyncEvent<T: Data>(
    override var sender: SendSource,
    var clazz: KClass<T>
): Event(sender, "sync"){

    override fun clone(): Event {
        return this.copy()
    }
}