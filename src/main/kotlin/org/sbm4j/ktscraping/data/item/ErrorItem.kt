package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Channelable
import org.sbm4j.ktscraping.data.Status

enum class ErrorLevel{
    MINOR,
    MAJOR,
    FATAL
}

data class ErrorInfo(
    val ex: Exception,
    val controllable: Controllable,
    val level: ErrorLevel,
    val message: String = ""
)

data class ErrorItem(
    val errorInfo: ErrorInfo,
    override var sender: Controllable,
    val data: Channelable? = null
): Item(){

    override val name: String = "ErrorItem"

    override fun clone(): Item {
        return this.copy()
    }

    override fun generateAck(
        status: Status,
        errors: MutableList<ErrorInfo>
    ): AbstractItemAck<ErrorItem>? {
        return null
    }

    override fun buildErrorBack(infos: ErrorInfo): Back<*> {
        TODO("Not yet implemented")
    }

}