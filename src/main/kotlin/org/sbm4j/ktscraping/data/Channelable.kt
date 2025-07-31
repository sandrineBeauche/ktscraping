package org.sbm4j.ktscraping.data

import org.sbm4j.ktscraping.data.item.ErrorInfo


interface Channelable {
}

enum class Status{
    OK,
    UNAUTHORIEZD,
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

interface Back: Channelable{
    var status: Status
    val errorInfos: MutableList<ErrorInfo>
}

interface EventBack: Back{
    val eventName: String
}