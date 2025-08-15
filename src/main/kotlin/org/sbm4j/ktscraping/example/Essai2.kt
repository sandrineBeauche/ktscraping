package org.sbm4j.ktscraping.example

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking



fun main(args: Array<String>): kotlin.Unit = runBlocking{
    coroutineScope {
        val channel: Channel<String> = Channel(Channel.UNLIMITED)

        launch{
            repeat(10){
                channel.send("from launch 1: #$it")
            }
            delay(1000L)
            repeat(10){
                println(channel.receive())
            }
        }
    }
}