package org.sbm4j.ktscraping.data

interface Event: Send {
    val eventName: String
}

interface StartEvent{}

interface EndEvent{}