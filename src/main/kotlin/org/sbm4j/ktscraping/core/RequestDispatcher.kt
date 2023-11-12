package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response

interface RequestDispatcher: Controllable {

    val receivers: HashMap<SendChannel<Request>, ReceiveChannel<Response>>

    val channelOut: SendChannel<Response>

    val channelIn: ReceiveChannel<Request>


    suspend fun performRequests(){
        scope.launch{
            for(request in channelIn){
                val channel = selectChannel(request)
                channel.send(request)
            }
        }
    }

    fun selectChannel(request: Request): SendChannel<Request>

    suspend fun performResponses(){
        scope.launch {
            select {
                for(ch in receivers.values){
                    ch.onReceiveCatching{ it ->
                        val value = it.getOrNull()
                        channelOut.send(value!!)
                    }
                }
            }
        }
    }
}