package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.data.Channelable

enum class ErrorLevel{
    MINOR,
    MAJOR,
    FATAL
}

data class ErrorInfo(
    val ex: Exception,
    val controllable: Controllable,
    val level: ErrorLevel
)

data class ItemError(
    val errorInfo: ErrorInfo,
    val data: Channelable? = null
): Item(){
    override fun clone(): Item {
        return this.copy()
    }

}