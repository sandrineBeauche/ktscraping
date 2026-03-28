package org.sbm4j.ktscraping.core.channels

import org.sbm4j.meercat.channels.ChannelManager
import org.sbm4j.meercat.channels.SuperChannel

class CrawlerChannelManager(): ChannelManager() {
    val spiderChannel: SuperChannel = SuperChannel("SuperChannel-spider")
    val downloaderChannel: SuperChannel = SuperChannel("SuperChannel-downloader")
    val pipelineChannel: SuperChannel = SuperChannel("SuperChannel-pipeline")

    init {
        this.channels.add(spiderChannel)
        this.channels.add(downloaderChannel)
        this.channels.add(pipelineChannel)
    }
}