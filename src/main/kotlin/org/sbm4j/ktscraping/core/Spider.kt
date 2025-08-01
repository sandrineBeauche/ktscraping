package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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


class SpiderStepException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
     constructor(cause: Throwable) : this(null, cause)
}

abstract class AbstractSpider(
     override val name: String = "Spider"
): RequestSender{

     override val mutex: Mutex = Mutex()
     override var state: State = State()
     override var pendingRequests: PendingRequestMap = PendingRequestMap()
     override val pendingEventJobs: ConcurrentHashMap<String, EventJobResult> = ConcurrentHashMap()

     override lateinit var requestOut: SendChannel<AbstractRequest>
     override lateinit var responseIn: ReceiveChannel<Response<*>>

     lateinit var itemsOut: SendChannel<Item>

     /**
      * Performs the scraping logic. Here the user writes his code to scrape what he wants
      * @param subScope the subscope where the scraping should be executed
      * @throws RequestException if there is an exception during the scraping
      */
     abstract suspend fun performScraping(subScope: CoroutineScope)

     override lateinit var scope: CoroutineScope

     lateinit var job: Job

     override val pendingMinorError: ConcurrentHashMap<Int, MutableList<ErrorInfo>> = ConcurrentHashMap()

     override suspend fun run() {
          logger.info{"${name}: Starting spider"}
          super.run()
          job = scope.launch {
               try{
                    logger.info{ "${name}: send start event request to initialize the crawler"}
                    val startRequest = StartRequest(this@AbstractSpider)
                    sendSync(startRequest, this)

                    logger.info{"${name}: Crawler initialized with success... start performing scraping"}
                    performScraping(this)
               }
               catch (ex: RequestException){
                    logger.error { "${name}: Error when running the spider -> ${ex.message}" }
                    val error = ErrorItem(ErrorInfo(ex, this@AbstractSpider, ErrorLevel.MAJOR))
                    itemsOut.send(error)
               }
               finally {
                    logger.info{"${name}: finished performing scraping... send end event request"}

                    val endRequest = EndRequest(this@AbstractSpider)
                    val endResp = sendSync(endRequest, this)
                    logger.info{ "${name}: ready to stop: ${endResp}"}
               }
          }
     }


     override suspend fun performDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest) {
          throw NoRequestSenderException("${name}: request ${response.request.name} is not correlated to a sender")
     }


     override suspend fun stop() {
          this.itemsOut.close()
          super.stop()
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
          try{
               val itemStart = StartTaskProgressItem(taskSlot, taskMessage, nbSteps, slotMode)
               itemsOut.send(itemStart)
               val result = func(task)
               return result
          }
          catch(ex: Exception){
               if(optional){
                    logger.error(ex){"${ex.message}"}
                    val error = ErrorItem(ErrorInfo(ex, this, ErrorLevel.MINOR))
                    itemsOut.send(error)
               }
               else{
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
          ){
               val scrapingStep = ScrapingStep()
               try{
                    val itemStart = StartStepProgressItem(slotId, stepMessage)
                    itemsOut.send(itemStart)
                    func(scrapingStep)
               }
               catch(ex: Exception){
                    val message = "in task ${slotId}-${taskName}, step ${stepMessage}: ${ex.message}"
                    val newEx = SpiderStepException(message, ex)
                    if(optional){
                         logger.error(newEx){"${name}: ${message}"}
                         val error = ErrorItem(ErrorInfo(newEx, this@AbstractSpider, ErrorLevel.MINOR))
                         itemsOut.send(error)
                    }
                    else{
                         throw newEx
                    }
               }
               finally {
                    val itemDone = StepDoneProgressItem(slotId, step)
                    itemsOut.send(itemDone)
               }
          }

          suspend fun stepGroup(
               func: suspend AbstractSpider.(task: ScrapingTask) -> Unit
          ){
               try{
                    func(this)
               }
               catch(ex: Exception){
                    logger.error(ex){"${ex.message}"}
                    val error = ErrorItem(ErrorInfo(ex, this@AbstractSpider, ErrorLevel.MINOR))
                    itemsOut.send(error)
               }
          }



          suspend inline fun <reified T: Data> sendData(data: T, label: String = "data"){
               val item = ObjectDataItem.build(data, label)
               itemsOut.send(item)
          }

     }

     inner class ScrapingStep(){
          suspend fun sendData(data: Data, label: String = "data"){
               val item = ObjectDataItem.build(data, label)
               itemsOut.send(item)
          }

          suspend fun sendUpdate(update: ItemUpdate){
               itemsOut.send(update)
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
          logger.info { "${name} sends a new request ${req.name}" }
          try {
               val resp = this.sendSync(req)

               parse(resp as DownloadingResponse)
          }
          catch(ex: Throwable){
               callbackError(ex)
          }
     }

     abstract suspend fun parse(resp: DownloadingResponse)

     abstract suspend fun callbackError(ex: Throwable)
}