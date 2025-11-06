package org.sbm4j.ktscraping.core.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.SlotMode
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.processors.SendException
import org.sbm4j.ktscraping.core.processors.SendSource
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.*
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.exporters.ItemUpdate


class SpiderStepException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}

abstract class AbstractSpider(
    override val name: String = "Spider"
) : SendSource, AbstractControllable() {



    override lateinit var outChannel: SuperChannel

    /**
     * Performs the scraping logic. Here the user writes his code to scrape what he wants
     * @param subScope the subscope where the scraping should be executed
     * @throws SendException if there is an exception during the scraping
     */
    abstract suspend fun performScraping(subScope: CoroutineScope)


    override suspend fun run() {
        logger.info { "${name}: Starting spider" }
        job = scope.launch {
            try {
                logger.info { "${name}: send start event to initialize the crawler" }
                val startEvent = StartEvent(this@AbstractSpider)
                val startEventBack = sendSync<Event>(startEvent, this)
                logger.debug{"${name}: received start event back: ${startEventBack}"}

                logger.info { "${name}: Crawler initialized with success... start performing scraping" }
                performScraping(this)
            } catch (ex: SendException) {
                logger.error { "${name}: Error when running the spider -> ${ex.message}" }
                val errorInfos = generateErrorInfos(ex)
                val error = ErrorInternal(errorInfos, this@AbstractSpider)
                outChannel.send(error)
            } finally {
                logger.info { "${name}: finished performing scraping... send end event" }

                val endEvent = EndEvent(this@AbstractSpider)
                val endEventBack = sendSync<Event>(endEvent, this)
                logger.info { "${name}: ready to stop: ${endEventBack}" }
            }
        }
    }


    override suspend fun stop() {
        this.outChannel.close()
    }

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


    inner class ScrapingTask(val slotId: String, val taskName: String, val nbSteps: Int) {


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


        suspend inline fun <reified T : Data> sendData(data: T, label: String = "data") {
            val item = ObjectDataItem.build(data, label, this@AbstractSpider)
            outChannel.send(item)
        }

    }

    inner class ScrapingStep() {
        suspend fun sendData(data: Data, label: String = "data") {
            val item = ObjectDataItem.build(data, label, this@AbstractSpider)
            outChannel.send(item)
        }

        suspend fun sendUpdate(update: ItemUpdate) {
            outChannel.send(update)
        }
    }
}


abstract class AbstractSimpleSpider(
    name: String = "Spider"
) : AbstractSpider(name) {

    lateinit var urlRequest: String

    override suspend fun performScraping(subScope: CoroutineScope) {
        val req = Request(this, urlRequest)
        logger.info { "$name sends a new request ${req.name}" }
        try {
            val resp = this.sendSync(req)

            parse(resp as DownloadingResponse)
        } catch (ex: Throwable) {
            callbackError(ex)
        }
    }

    abstract suspend fun parse(resp: DownloadingResponse)

    abstract suspend fun callbackError(ex: Throwable)
}