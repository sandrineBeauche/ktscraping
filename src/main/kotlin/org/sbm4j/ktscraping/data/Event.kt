package org.sbm4j.ktscraping.data

interface Event {
    val eventName: String
}

interface StartEvent{}

interface EndEvent{}