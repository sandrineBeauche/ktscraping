package org.sbm4j.ktscraping.data.internal

import org.sbm4j.meercat.data.Channelable
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

/**
 * Internal message reporting an error that occurred during the scraping process.
 *
 * Emitted by a Spider when an error is caught during scraping, carrying the full
 * error details and optionally the [Channelable] message that was being processed
 * when the error occurred.
 *
 * The barrier key is the simple name of the exception class, allowing [Barrier] nodes
 * to synchronize branches by error type.
 *
 * @property errorInfo Details of the error that occurred, including exception, node, level and message.
 * @property sender The Spider emitting this notification.
 * @property data The [Channelable] message being processed when the error occurred, if any.
 *
 * @see ErrorInfo
 * @see Internal
 */
data class ErrorInternal(
    val errorInfo: ErrorInfo,
    override var sender: SendSource,
    val data: Channelable? = null
): Internal(){


    override val name: String = "ErrorInternalData-${Channelable.lastId.getAndIncrement()}"

    /** Creates a copy of this message via [copy]. */
    override fun clone(): Send {
        return this.copy()
    }

    /**
     * Returns the simple name of the exception class as the barrier key,
     * allowing [Barrier] nodes to synchronize branches by error type.
     */
    override fun getKeyBarrier(): String {
        return this.errorInfo.ex.javaClass.simpleName
    }
}
