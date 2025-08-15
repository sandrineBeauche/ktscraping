package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.util.UUID
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.item.*
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.request.EndRequest
import org.sbm4j.ktscraping.data.request.Request
import org.sbm4j.ktscraping.data.request.StartRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.exporters.ItemUpdate
import java.util.concurrent.ConcurrentHashMap
import org.sbm4j.ktscraping.data.response.Response
import kotlin.reflect.KClass


class SpiderStepException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}

abstract class AbstractSpider(
    override val name: String = "Spider"
) : SendSource<AbstractRequest, Back<AbstractRequest>>, RequestSender, Controllable {

    override val mutex: Mutex = Mutex()
    override var state: State = State()

    override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

    override lateinit var outChannel: SuperChannel

    override val backClazz: KClass<*> = Back::class

    override val sendClazz: KClass<*> = Send::class

    override val pendingMinorError: ConcurrentHashMap<UUID, MutableList<ErrorInfo>>
        get() = TODO("Not yet implemented")

    override suspend fun performBack(back: Back<AbstractRequest>) {
        super<RequestSender>.performBack(back as Response<*>)
    }

    override suspend fun processDownloadingResponse(
        response: DownloadingResponse,
        request: DownloadingRequest
    ): Boolean {
        return false
    }

    /**
     * Performs the scraping logic. Here the user writes his code to scrape what he wants
     * @param subScope the subscope where the scraping should be executed
     * @throws SendException if there is an exception during the scraping
     */
    abstract suspend fun performScraping(subScope: CoroutineScope)

    override lateinit var scope: CoroutineScope

    lateinit var job: Job


    override suspend fun run() {
        logger.info { "${name}: Starting spider" }
        job = scope.launch {
            try {
                logger.info { "${name}: send start event request to initialize the crawler" }
                val startRequest = StartRequest(this@AbstractSpider)
                sendSync(startRequest, this)


                logger.info { "${name}: Crawler initialized with success... start performing scraping" }
                performScraping(this)
            } catch (ex: SendException) {
                logger.error { "${name}: Error when running the spider -> ${ex.message}" }
                val error = ErrorItem(
                    ErrorInfo(ex, this@AbstractSpider, ErrorLevel.MAJOR),
                    this@AbstractSpider
                )
                outChannel.send(error)
            } finally {
                logger.info { "${name}: finished performing scraping... send end event request" }

                val endRequest = EndRequest(this@AbstractSpider)
                val endResp = sendSync(endRequest, this)
                logger.info { "${name}: ready to stop: ${endResp}" }
            }
        }
    }


    override suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
        throw NoRequestSenderException("${name}: request ${response.send.name} is not correlated to a sender")
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
            val itemStart = StartTaskProgressItem(taskSlot, taskMessage, nbSteps, slotMode, this@AbstractSpider)
            outChannel.send(itemStart)
            val result = func(task)
            return result
        } catch (ex: Exception) {
            if (optional) {
                logger.error(ex) { "${ex.message}" }
                val error = ErrorItem(
                    ErrorInfo(ex, this, ErrorLevel.MINOR),
                    this
                )
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
                    val error = ErrorItem(
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
                val error = ErrorItem(
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


    override fun generateErrorInfos(ex: Exception): ErrorInfo {
        return ErrorInfo(ex, this, ErrorLevel.MAJOR)
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