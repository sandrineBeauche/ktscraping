package org.sbm4j.ktscraping.data

import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.data.item.ErrorInfo
import java.util.*


interface Channelable {
    var channelableId: UUID

    val loggingLabel: String
        get() = "${this::class.simpleName}"

    val name: String
}

interface Send: Channelable{
    var sender: Controllable

    fun buildErrorBack(infos: ErrorInfo): Back<*>
}

enum class Status{
    OK,
    UNAUTHORIZED,
    NOT_FOUND,
    ERROR,
    IGNORED;

    operator fun plus(other: Status): Status {
        return when (this to other) {
            OK to OK -> OK
            OK to ERROR, ERROR to OK -> ERROR
            else -> this // Default behavior
        }
    }
}

interface Back<T: Send>: Channelable{
    val send: T
    var status: Status
    val errorInfos: MutableList<ErrorInfo>
}

interface EventBack<T: Event>: Back<T>{
    val eventName: String
        get() = send.eventName
}