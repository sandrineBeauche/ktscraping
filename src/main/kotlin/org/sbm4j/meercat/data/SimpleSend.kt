package org.sbm4j.meercat.data

import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * A simple generic implementation of [Send] carrying a value of type [T].
 *
 * Designed for straightforward use cases and tests where a lightweight [Send]
 * implementation is needed without custom processing logic.
 *
 * @param T the type of the value carried by this send
 * @property value the value carried by this send
 * @property sender the [SendSource] that emitted this send
 */
 data class SimpleSend<T>(var value: T, override var sender: SendSource) : Send{
    companion object{
        /**
         * Counter used to generate unique [name] values across all [SimpleSend] instances.
         */
        var lastId: AtomicInteger = AtomicInteger(0)
    }

    /**
     * @see Send.channelableId
     */
    override var channelableId: UUID = UUID.randomUUID()

    /**
     * Unique human-readable name derived from a global counter, used for logging.
     *
     * @see Send.name
     */
    override val name: String = "SimpleSend#${lastId.getAndIncrement()}"

    /**
     * @see Send.buildErrorBack
     */
    override fun buildErrorBack(
        infos: ErrorInfo,
        status: Status
    ): Back<*> {
        val result = SimpleBack(this)
        result.status = status
        result.errorInfos.add(infos)
        return result
    }

    /**
     * @see Send.buildBack
     */
    override fun buildBack(): Back<*> {
        return SimpleBack(this)
    }

    /**
     * @see Send.clone
     */
    override fun clone(): Send {
        return this.copy()
    }
}

/**
 * A simple generic implementation of [Back] carrying a response to a [SimpleSend] of type [T].
 *
 * Designed for straightforward use cases and tests where a lightweight [Back]
 * implementation is needed without custom processing logic.
 *
 * @param T the type of the value carried by the original [SimpleSend]
 * @property send the [SimpleSend] this back is responding to
 */
data class SimpleBack<T>(override val send: SimpleSend<T>): Back<SimpleSend<T>> {
    companion object{
        /**
         * Counter used to generate unique [name] values across all [SimpleBack] instances.
         */
        var lastId: AtomicInteger = AtomicInteger(0)
    }

    /**
     * @see Channelable.channelableId
     */
    override var channelableId: UUID = UUID.randomUUID()

    /**
     * Unique human-readable name derived from a global counter, used for logging.
     *
     * @see Channelable.name
     */
    override val name: String = "SimpleBack#${lastId.getAndIncrement()}"

    /**
     * @see Back.status
     */
    override var status: Status = Status.OK

    /**
     * @see Back.errorInfos
     */
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()

    /**
     * @see Back.clone
     */
    override fun clone(): Back<SimpleSend<T>> {
        return this.copy()
    }
}

/**
 * A [SimpleSend] carrying a [String] value, for straightforward use cases and tests.
 */
typealias StringSend = SimpleSend<String>

/**
 * A [SimpleBack] carrying a response to a [StringSend], for straightforward use cases and tests.
 */
typealias StringBack = SimpleBack<String>