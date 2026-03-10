package org.sbm4j.ktscraping.data.internal

import org.sbm4j.meercat.components.Controllable
import org.sbm4j.meercat.channels.Channelable
import org.sbm4j.meercat.components.SendSource

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
    override var sender: SendSource,
    val data: Channelable? = null
): Internal(){

    override val name: String = "ErrorInternalData-${Channelable.lastId.getAndIncrement()}"

    override fun clone(): ErrorInternal {
        return this.copy()
    }
}
