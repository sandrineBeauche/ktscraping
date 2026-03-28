package org.sbm4j.meercat.nodes

import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.sendProcessors.SendForwarder

abstract class AbstractMiddleNode(
    override val name: String
): AbstractProcessingNode(), SendForwarder, BackForwarder {
    override lateinit var outChannel: SuperChannel

    override lateinit var inChannel: SuperChannel

}