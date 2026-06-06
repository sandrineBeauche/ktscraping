package org.sbm4j.ktscraping.data.events

import org.sbm4j.meercat.nodes.sendProcessors.SendSource

/**
 * Start event of the KtScraping topology.
 *
 * First event emitted by the Spider, signaling to all branches
 * that they can proceed with their initialization. Propagates to both branches
 * by default ([EventPropagation.BOTH]).
 *
 * @param sender The Spider initiating the scraping session.
 */
data class StartEvent(
    override var sender: SendSource
): Event(sender, "start"){

    override fun clone(): Event {
        return this.copy()
    }
}

/**
 * End event of the KtScraping topology.
 *
 * Last event emitted by the Spider, signaling to all branches that they can
 * proceed with their teardown operations (flushing buffers, closing connections,
 * final export, etc.). Propagates to both branches by default ([EventPropagation.BOTH]).
 *
 * @param sender The Spider ending the scraping session.
 */
data class EndEvent(
    override var sender: SendSource
): Event(sender, "end"){

    override fun clone(): Event {
        return this.copy()
    }
}