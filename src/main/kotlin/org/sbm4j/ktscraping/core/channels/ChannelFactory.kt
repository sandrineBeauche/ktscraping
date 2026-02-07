package org.sbm4j.ktscraping.core.channels

class ChannelFactory {
    val spiderChannel: SuperChannel = SuperChannel("SuperChannel-spider")
    val downloaderChannel: SuperChannel = SuperChannel("SuperChannel-downloader")
    val pipelineChannel: SuperChannel = SuperChannel("SuperChannel-pipeline")

    suspend fun initChannels(){
        for(channel in listOf(spiderChannel, downloaderChannel, pipelineChannel)){
            channel.init()
        }
    }

    fun closeChannels(){
        for(channel in listOf(spiderChannel, downloaderChannel, pipelineChannel)){
            channel.close()
        }
    }
}