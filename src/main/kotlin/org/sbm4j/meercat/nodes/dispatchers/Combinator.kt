package org.sbm4j.meercat.nodes.dispatchers

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.Node
import org.sbm4j.meercat.nodes.logger

/**
 * Represents a node in the Meercat topology that can receive [Send] messages from multiple
 * input channels and forward them to a single output channel, then dispatch the [Back] responses
 * back to the appropriate input channel.
 *
 * A [Combinator] sits at a branching point in the topology, managing multiple [channelsIns]
 * as entry points and a single [channelOut] as exit point. It can handle different types of
 * messages with different behaviours, by registering multiple collectors with distinct predicates
 * on the same node.
 *
 * Concrete behaviours are defined by sub-interfaces, the two main ones being:
 * - **Barrier**: accumulates messages from different branches that share a common key
 *   (see [Send.getKeyBarrier]), then forwards a combined message on [channelOut] once all
 *   expected messages have arrived. The [Back] response is then dispatched back to all
 *   originating branches.
 * - **Dispatcher**: forwards a message received on one of the [channelsIns] to [channelOut],
 *   memorises the association between the message and its originating branch, then dispatches
 *   the [Back] response back to the correct [SuperChannel] in [channelsIns].
 *
 * A single [Combinator] node can cumulate multiple behaviours by registering several
 * [performSendBacks] calls with different predicates, each handled by a dedicated pair
 * of send and back collectors. Sub-interfaces define their own specifically named processing
 * methods to avoid naming conflicts when multiple behaviours are combined in the same node.
 *
 * For consistency, every [performSends] registration with a given predicate must have a
 * matching [performBacks] registration with the exact same predicate. [performSendBacks]
 * enforces this by registering both collectors at once.
 */
interface Combinator: Node {

    /**
     * The list of input [SuperChannel] instances from which this node receives [Send] messages.
     * Each channel typically corresponds to a distinct branch of the topology.
     */
    val channelsIns : MutableList<SuperChannel>

    /**
     * The output [SuperChannel] through which this node forwards [Send] messages
     * and receives [Back] responses from downstream nodes.
     */
    val channelOut: SuperChannel

    /**
     * Registers a collector on each channel in [channelsIns] that processes incoming [Send]
     * messages concurrently, optionally filtered by [predicate].
     *
     * Each collector is launched in a dedicated coroutine. For each received [Send] message,
     * [perform] is called with the message, the originating [SuperChannel], and its index
     * in [channelsIns], allowing the implementation to identify which branch the message
     * came from.
     *
     * This method is typically not called directly — use [performSendBacks] instead to ensure
     * a matching [performBacks] is always registered with the same predicate.
     *
     * @param predicate an optional filter applied to incoming [Send] messages.
     * Only messages for which the predicate returns `true` are processed
     * @param perform the processing function applied to each received [Send] message,
     * receiving the message, its originating [SuperChannel], and its index in [channelsIns]
     */
    suspend fun performSends(
        predicate: (suspend (Send) -> Boolean)? = null,
        perform: suspend (Send, SuperChannel, Int) -> Unit
    ) {
        repeat(channelsIns.size) {
            collectReadyLatch.increment()
        }
        for ((index, channel) in channelsIns.withIndex()) {
            val coroutineName = "${name}-performSends-${index}"
            scope.launch(CoroutineName(coroutineName)) {
                val flow = channel.getSendFlow()
                val filtered = if (predicate != null) {
                    flow.filter(predicate)
                } else flow
                filtered
                    .onStart {
                        logger.trace { "${name}: flow started on coroutine $coroutineName" }
                        collectReadyLatch.signal()
                    }
                    .collect { send ->
                        perform(send, channel, index)
                    }
            }
        }
    }

    /**
     * Registers a collector on [channelOut] that processes incoming [Back] responses,
     * optionally filtered by a predicate applied to the original [Send] message.
     *
     * This method is typically not called directly — use [performSendBacks] instead to ensure
     * a matching [performSends] is always registered with the same predicate.
     *
     * @param predicate an optional filter applied to the original [Send] of each incoming [Back].
     * Only responses whose original [Send] matches the predicate are processed
     * @param perform the processing function applied to each received [Back] response
     */
    suspend fun performBacks(
        predicate: (suspend (Send) -> Boolean)? = null,
        perform: suspend (Back<*>) -> Unit
    ) {
        collectReadyLatch.increment()
        val coroutineName = "${name}-performBacks"
        scope.launch(CoroutineName(coroutineName)) {
            val flow = channelOut.getBackFlow()
            val filtered = if(predicate != null) {
                flow.filter { back -> predicate(back.send) }
            }
            else flow
            filtered
                .onStart {
                    logger.trace { "${name}: flow started on coroutine $coroutineName" }
                    collectReadyLatch.signal()
                }
                .collect { back ->
                    perform(back)
            }
        }
    }

    /**
     * Registers both a send collector and a back collector with the same [predicate],
     * ensuring consistency between the forward and return paths for a given type of message.
     *
     * Implementations are provided by sub-interfaces, each defining their own specifically
     * named processing methods to avoid naming conflicts when multiple behaviours are
     * combined in the same node.
     *
     * @param predicate an optional filter applied to [Send] messages and to the original [Send]
     * of [Back] responses, ensuring both collectors handle the same subset of messages
     */
    suspend fun performSendBacks(predicate: (suspend (Send) -> Boolean)? = null)
}