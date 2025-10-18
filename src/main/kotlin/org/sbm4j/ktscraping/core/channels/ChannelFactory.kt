package org.sbm4j.ktscraping.core.channels

class ChannelFactory {
    val spiderChannel: SuperChannel = SuperChannel()
    val downloaderChannel: SuperChannel = SuperChannel()
    val pipelineChannel: SuperChannel = SuperChannel()

    fun closeChannels(){
        for(channel in listOf(spiderChannel, downloaderChannel, pipelineChannel)){
            channel.close()
        }
    }
}