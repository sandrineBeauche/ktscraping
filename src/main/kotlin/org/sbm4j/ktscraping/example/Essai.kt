package org.sbm4j.ktscraping.example

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): kotlin.Unit = runBlocking{
    val channel: Channel<String> =  Channel(Channel.UNLIMITED)

    coroutineScope {
        launch{
            repeat(10){index ->
                channel.send("coucou-$index")
                channel.send("bonjour-$index")
            }
        }

        launch {
            channel.receiveAsFlow().takeIf { (it as String).contains("coucou") }?.collect { println("inside receive as flow1: $it") }
        }
        launch {
            channel.receiveAsFlow().collect { println("inside receive as flow2: $it") }
        }
    }
}