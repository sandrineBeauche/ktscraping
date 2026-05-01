package org.sbm4j.ktscraping.data.internal

import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.data.Status
import java.util.*

/**
 * Base message representing an internal notification in the KtScraping topology.
 *
 * Internal messages are emitted by Spiders to report scraping progress or errors
 * to the Engine, which aggregates them to feed statistics or forward them to a
 * monitoring channel (e.g. for a UI progress bar or a dashboard).
 *
 * Unlike [Event], Internal messages are fire-and-forget: they do not expect any back
 * from their consumers and are purely unidirectional.
 *
 * @see Event
 */
abstract class Internal: Send {

    override var channelableId: UUID = UUID.randomUUID()

    /**
     * Not supported for [Internal] messages.
     * Internal messages are fire-and-forget and never expect a back.
     * @throws UnsupportedOperationException always
     */
    override fun buildBack(): Back<*> =
        throw UnsupportedOperationException("Internal messages do not support backs")

    /**
     * Not supported for [Internal] messages.
     * Internal messages are fire-and-forget and never expect a back.
     * @throws UnsupportedOperationException always
     */
    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> =
        throw UnsupportedOperationException("Internal messages do not support backs")

}




