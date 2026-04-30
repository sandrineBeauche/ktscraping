package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.SlotMode
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInternal
import org.sbm4j.ktscraping.data.internal.StartStepProgressItem
import org.sbm4j.ktscraping.data.internal.StartTaskProgressInternal
import org.sbm4j.ktscraping.data.internal.StepDoneProgressItem
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ItemAck
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel
import org.sbm4j.meercat.data.SendException
import org.sbm4j.meercat.nodes.logger

/**
 * Exception thrown during a scraping step to wrap and contextualize errors.
 *
 * Wraps the original exception with a message indicating the task and step
 * where the error occurred, for easier debugging and error reporting.
 *
 * @param message Optional error message.
 * @param cause The original exception that caused this error.
 */
class SpiderStepException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}

/**
 * Base abstract class for Spider nodes — the [Initiator] of the KtScraping topology.
 *
 * A Spider drives the entire scraping session by:
 * 1. Emitting a [StartEvent] to initialize all downstream components.
 * 2. Executing [performScraping] to perform the actual scraping logic.
 * 3. Emitting an [EndEvent] to trigger teardown of all downstream components.
 *
 * Any [SendException] during scraping is caught and reported as an [ErrorInternal]
 * to the Engine before the [EndEvent] is sent.
 *
 * Scraping logic is structured using the [task] DSL, which organizes work into
 * named tasks ([ScrapingTask]) and steps ([ScrapingStep]), with automatic progress
 * reporting via [ProgressInternal] messages.
 *
 * @param name The name of this spider, defaults to `"Spider"`.
 *
 * @see AbstractInitiatorComponent
 * @see ScrapingTask
 * @see ScrapingStep
 * @see AbstractSimpleSpider
 */
abstract class AbstractSpider(
    override val name: String = "Spider"
) : AbstractInitiatorComponent() {

    /**
     * Performs the actual scraping logic.
     *
     * Implement this method to emit [AbstractRequest]s, receive [Response]s,
     * and send [Item]s to the Pipeline branch. Use the [task] DSL to structure
     * the work into tasks and steps with automatic progress reporting.
     */
    abstract suspend fun performScraping()


    override suspend fun run() {
        logger.info { "${name}: Starting spider" }
        try {
            logger.info { "${name}: send start event to initialize the crawler" }
            val startEvent = StartEvent(this@AbstractSpider)
            val startEventBack = sendSync<Event>(startEvent)
            logger.debug { "${name}: received start event back: ${startEventBack}" }

            logger.info { "${name}: Crawler initialized with success... start performing scraping" }
            performScraping()
        } catch (ex: SendException) {
            logger.error { "${name}: Error when running the spider -> ${ex.message}" }
            val errorInfos = generateErrorInfos(ex)
            val error = ErrorInternal(errorInfos, this@AbstractSpider)
            outChannel.send(error)
        } finally {
            logger.info { "${name}: finished performing scraping... send end event" }

            val endEvent = EndEvent(this@AbstractSpider)
            val endEventBack = sendSync<Event>(endEvent)
            logger.info { "${name}: ready to stop: ${endEventBack}" }
        }
        logger.debug { "${name}: done launching run in spider" }
    }


    override suspend fun stop() {
        super.stop()
    }

    /**
     * DSL entry point for structuring scraping work into a named task.
     *
     * Emits a [StartTaskProgressInternal] to initialize the progress slot, then
     * executes [func] in the context of a [ScrapingTask]. If an exception occurs
     * and [optional] is `true`, the error is reported as a [ErrorInternal] with
     * [ErrorLevel.MINOR] and execution continues. Otherwise the exception is rethrown.
     *
     * @param taskName Human-readable name of this task.
     * @param taskSlot The progress slot identifier, defaults to the spider name.
     * @param taskMessage Human-readable description displayed in the progress UI.
     * @param optional If `true`, exceptions are caught and reported without interrupting scraping.
     * @param nbSteps Total number of steps expected for this task.
     * @param slotMode Display mode for the progress slot, defaults to [SlotMode.PROGRESS_BAR_DEFINED].
     * @param func The scraping logic to execute in the context of a [ScrapingTask].
     * @return The result of [func], or `null` if an optional error occurred.
     */
    suspend fun <T> task(
        taskName: String,
        taskSlot: String = name,
        taskMessage: String = "",
        optional: Boolean = false,
        nbSteps: Int = 1,
        slotMode: SlotMode = SlotMode.PROGRESS_BAR_DEFINED,
        func: suspend AbstractSpider.(task: ScrapingTask) -> T
    ): T? {
        val task = ScrapingTask(taskSlot, taskName, nbSteps)
        try {
            val itemStart = StartTaskProgressInternal(taskSlot, taskMessage, nbSteps, slotMode, this@AbstractSpider)
            outChannel.send(itemStart)
            val result = func(task)
            return result
        } catch (ex: Exception) {
            if (optional) {
                logger.error(ex) { "${ex.message}" }
                val error = ErrorInternal(this.generateErrorInfos(ex, ErrorLevel.MINOR), this)
                outChannel.send(error)
            } else {
                throw ex
            }
        }
        return null
    }

    /**
     * Represents a named scraping task within the [task] DSL.
     *
     * Provides [step] and [stepGroup] to organize work into individual steps,
     * and [sendData] to emit scraped [Data] items to the Pipeline branch.
     * Progress is automatically reported via [ProgressInternal] messages.
     *
     * @param slotId The progress slot identifier for this task.
     * @param taskName The name of this task.
     * @param nbSteps Total number of steps expected for this task.
     */
    inner class ScrapingTask(val slotId: String, val taskName: String, val nbSteps: Int) {

        /**
         * Executes a single scraping step within this task.
         *
         * Emits a [StartStepProgressItem] before executing [func], and a
         * [StepDoneProgressItem] afterward (even on error). If an exception occurs
         * and [optional] is `true`, the error is reported as an [ErrorInternal] with
         * [ErrorLevel.MINOR] and execution continues. Otherwise the exception is
         * wrapped in a [SpiderStepException] and rethrown.
         *
         * @param stepMessage Human-readable description of this step.
         * @param optional If `true`, exceptions are caught and reported without interrupting the task.
         * @param step Number of steps to mark as done upon completion, defaults to `1`.
         * @param func The scraping logic to execute in the context of a [ScrapingStep].
         */
        suspend fun step(
            stepMessage: String,
            optional: Boolean = false,
            step: Int = 1,
            func: suspend AbstractSpider.(step: ScrapingStep) -> Unit
        ) {
            val scrapingStep = ScrapingStep()
            try {
                val itemStart = StartStepProgressItem(slotId, stepMessage, this@AbstractSpider)
                outChannel.send(itemStart)
                func(scrapingStep)
            } catch (ex: Exception) {
                val message = "in task ${slotId}-${taskName}, step ${stepMessage}: ${ex.message}"
                val newEx = SpiderStepException(message, ex)
                if (optional) {
                    logger.error(newEx) { "${name}: $message" }
                    val error = ErrorInternal(
                        ErrorInfo(newEx, this@AbstractSpider, ErrorLevel.MINOR),
                        this@AbstractSpider
                    )
                    outChannel.send(error)
                } else {
                    throw newEx
                }
            } finally {
                val itemDone = StepDoneProgressItem(slotId, step, this@AbstractSpider)
                outChannel.send(itemDone)
            }
        }

        /**
         * Groups multiple steps together, catching any exception and reporting it
         * as an [ErrorInternal] with [ErrorLevel.MINOR] without interrupting the task.
         *
         * Use this to batch optional steps that should not fail the entire task
         * if one of them throws.
         *
         * @param func The grouped scraping logic to execute in the context of this [ScrapingTask].
         */
        suspend fun stepGroup(
            func: suspend AbstractSpider.(task: ScrapingTask) -> Unit
        ) {
            try {
                func(this)
            } catch (ex: Exception) {
                logger.error(ex) { "${ex.message}" }
                val error = ErrorInternal(
                    ErrorInfo(ex, this@AbstractSpider, ErrorLevel.MINOR),
                    this@AbstractSpider
                )
                outChannel.send(error)
            }
        }

        /**
         * Emits a scraped [Data] object as an [ObjectDataItem] to the Pipeline branch.
         *
         * @param T The [Data] subclass type, reified.
         * @param data The scraped data object to send.
         * @param label A descriptive label for the item, defaults to `"data"`.
         */
        suspend inline fun <reified T : Data> sendData(data: T, label: String = "data") {
            val item = ObjectDataItem.build(data, label, this@AbstractSpider)
            outChannel.sendSync<ItemAck>(item)
        }

    }

    /**
     * Represents a single scraping step within a [ScrapingTask].
     *
     * Provides [sendData] to emit scraped [Data] items and [sendUpdate] to emit
     * [ItemUpdate] messages for partial database updates without resending the full item.
     */
    inner class ScrapingStep() {

        /**
         * Emits a scraped [Data] object as an [ObjectDataItem] to the Pipeline branch.
         *
         * @param data The scraped data object to send.
         * @param label A descriptive label for the item, defaults to `"data"`.
         */
        suspend fun sendData(data: Data, label: String = "data") {
            val item = ObjectDataItem.build(data, label, this@AbstractSpider)
            outChannel.sendSync<ItemAck>(item)
        }

        /**
         * Emits an [ItemUpdate] to the Pipeline branch.
         *
         * Use this instead of [sendData] when only a partial update is needed
         * (e.g. updating a single field in a database record) without resending
         * the full data object.
         *
         * @param update The partial update to send.
         */
        suspend fun sendUpdate(update: ItemUpdate) {
            outChannel.sendSync<ItemAck>(update)
        }
    }
}

/**
 * A simplified Spider for single-URL scraping scenarios.
 *
 * Handles the most common scraping pattern: send a single [Request] for [urlRequest],
 * receive the [DownloadingResponse], and delegate parsing to [parse]. Any error
 * during the request or parsing is delegated to [callbackError].
 *
 * @param name The name of this spider, defaults to `"Spider"`.
 *
 * @see AbstractSpider
 * @see Request
 * @see DownloadingResponse
 */
abstract class AbstractSimpleSpider(
    name: String = "Spider"
) : AbstractSpider(name) {

    /** The URL to download and parse. Must be set before the spider starts. */
    lateinit var urlRequest: String


    override suspend fun performScraping() {
        val req = Request(this, urlRequest)
        logger.info { "$name sends a new request ${req.name}" }
        try {
            val resp = this.sendSync<Request>(req)

            parse(resp as DownloadingResponse)
        } catch (ex: Throwable) {
            callbackError(ex)
        }
    }

    /**
     * Parses the [DownloadingResponse] and emits scraped items to the Pipeline branch.
     *
     * @param resp The response received from the Downloader.
     */
    abstract suspend fun parse(resp: DownloadingResponse)

    /**
     * Called when an error occurs during the request or parsing.
     *
     * @param ex The exception that was thrown.
     */
    abstract suspend fun callbackError(ex: Throwable)
}