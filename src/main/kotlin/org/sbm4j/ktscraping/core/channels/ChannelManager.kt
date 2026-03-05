package org.sbm4j.ktscraping.core.channels

import kotlinx.coroutines.CoroutineScope

class ChannelManager() {
    val spiderChannel: SuperChannel = SuperChannel("SuperChannel-spider")
    val downloaderChannel: SuperChannel = SuperChannel("SuperChannel-downloader")
    val pipelineChannel: SuperChannel = SuperChannel("SuperChannel-pipeline")

    val channels: MutableList<SuperChannel> = mutableListOf(spiderChannel, downloaderChannel, pipelineChannel)

    var channelIndex = 0

    var ready = false

    fun initChannels(parentScope: CoroutineScope) {
        if(!ready) {
            for (channel in channels) {
                channel.init(parentScope)
            }
            ready = true
        }
    }

    fun buildChannel(): SuperChannel {
        val newChannel = SuperChannel("SuperChannel-$channelIndex")
        channelIndex++
        channels.add(newChannel)
        return newChannel
    }

    fun closeChannels(){
        for(channel in channels){
            channel.close()
        }
    }
}