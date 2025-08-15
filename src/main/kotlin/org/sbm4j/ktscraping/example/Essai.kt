package org.sbm4j.ktscraping.example

import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): kotlin.Unit = runBlocking{
    val channel: Channel<Any> = Channel(Channel.UNLIMITED)


    coroutineScope {

        launch{
            repeat(10){
                channel.send("coucou-$it")
                channel.send(it)
            }
            println("close channel")
            channel.close()
        }
        launch{
            val flow = channel.consumeAsFlow().shareIn(this,
                SharingStarted.WhileSubscribed())
            val result = flow.filterIsInstance<Int>().first({it == 3})
            println("inside filter int: $result")
            println("end of int")
            this.cancel()
        }

    }

    println("end global")
}