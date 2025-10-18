package org.sbm4j.ktscraping.data.internal

import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.data.Channelable

data class ErrorInfo(
    val ex: Exception,
    val controllable: Controllable,
    val level: ErrorLevel,
    val message: String = ""
)

enum class ErrorLevel{
    MINOR,
    MAJOR,
    FATAL
}



data class ErrorInternal(
    val errorInfo: ErrorInfo,
    override var sender: Controllable,
    val data: Channelable? = null
): Internal(){

    override val name: String = "ErrorInternalData-${Channelable.lastId.getAndIncrement()}"

    override fun clone(): ErrorInternal {
        return this.copy()
    }
}
