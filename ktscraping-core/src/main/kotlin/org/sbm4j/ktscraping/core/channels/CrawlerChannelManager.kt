package org.sbm4j.ktscraping.core.channels

import org.sbm4j.meercat.channels.ChannelManager
import org.sbm4j.meercat.channels.SuperChannel

/**
 * Meercat [ChannelManager] for the KtScraping topology.
 *
 * Declares and registers the three [SuperChannel]s that form the backbone
 * of the KtScraping topology:
 * - [spiderChannel]: connects the Spider branch to the [SpiderDispatcher] and [AbstractEngine].
 * - [downloaderChannel]: connects the [AbstractEngine] to the Downloader branch.
 * - [pipelineChannel]: connects the [AbstractEngine] to the Pipeline branch.
 *
 * All three channels are automatically registered in the [ChannelManager] at construction,
 * ensuring they are initialized and ready before the topology starts.
 *
 * @see ChannelManager
 * @see AbstractEngine
 * @see SpiderDispatcher
 */
class CrawlerChannelManager(): ChannelManager() {
    /** Channel between the Spider branch and the [AbstractEngine]. */
    val spiderChannel: SuperChannel = SuperChannel("SuperChannel-spider")

    /** Channel between the [AbstractEngine] and the Downloader branch. */
    val downloaderChannel: SuperChannel = SuperChannel("SuperChannel-downloader")

    /** Channel between the [AbstractEngine] and the Pipeline branch. */
    val pipelineChannel: SuperChannel = SuperChannel("SuperChannel-pipeline")

    init {
        this.channels.add(spiderChannel)
        this.channels.add(downloaderChannel)
        this.channels.add(pipelineChannel)
    }
}