package org.sbm4j.ktscraping.core

import kotlinx.coroutines.channels.Channel
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.Response


class ChannelFactory {
    val spiderRequestChannel: Channel<AbstractRequest> = Channel(Channel.UNLIMITED)
    val spiderResponseChannel: Channel<Response<*>> = Channel(Channel.UNLIMITED)
    val spiderItemChannel: Channel<Item> = Channel(Channel.UNLIMITED)
    val downloaderRequestChannel: Channel<AbstractRequest> = Channel(Channel.UNLIMITED)
    val downloaderResponseChannel: Channel<Response<*>> = Channel(Channel.UNLIMITED)
    val itemChannel: Channel<Item> = Channel(Channel.UNLIMITED)
    val itemAckChannel: Channel<AbstractItemAck<*>> = Channel(Channel.UNLIMITED)

    val channels: MutableList<Channel<*>> = mutableListOf(
        spiderRequestChannel,
        spiderResponseChannel,
        spiderItemChannel,
        downloaderRequestChannel,
        downloaderResponseChannel,
        itemChannel
    )

    fun closeChannels(){
        for(channel in channels){
            if(!channel.isClosedForReceive || !channel.isClosedForSend) {
                channel.close()
            }
        }
    }
}