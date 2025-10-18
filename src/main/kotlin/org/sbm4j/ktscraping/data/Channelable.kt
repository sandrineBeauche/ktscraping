package org.sbm4j.ktscraping.data

import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


interface Channelable: Cloneable {
    companion object{
        val lastId = AtomicInteger(0)
    }

    var channelableId: UUID

    val loggingLabel: String
        get() = "${this::class.simpleName}"

    val name: String

    public override fun clone(): Channelable
}

interface Send: Channelable{
    var sender: Controllable

    fun buildErrorBack(infos: ErrorInfo, status: Status = Status.ERROR): Back<*>

    fun buildBack(): Back<*>

    override fun clone(): Send
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

    override fun clone(): Back<T>

    operator fun plus(increment: Back<*>): Back<*>{
        val result = this.clone()
        result.status += result.status + increment.status
        result.errorInfos.addAll(increment.errorInfos)
        return result
    }
}
