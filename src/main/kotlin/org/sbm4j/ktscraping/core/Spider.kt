package org.sbm4j.ktscraping.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.exporters.ItemUpdate
import org.sbm4j.ktscraping.requests.*
import kotlin.coroutines.CoroutineContext


class SpiderStepException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
     constructor(cause: Throwable) : this(null, cause)
}

abstract class AbstractSpider(
     override val name: String = "Spider"
): RequestSender{

     override val mutex: Mutex = Mutex()
     override var state: State = State()
     override var pendingRequests: PendingRequestMap = PendingRequestMap()

     override lateinit var requestOut: SendChannel<AbstractRequest>
     override lateinit var responseIn: ReceiveChannel<Response>

     lateinit var itemsOut: SendChannel<Item>

     abstract suspend fun performScraping(subScope: CoroutineScope)

     override lateinit var scope: CoroutineScope


     override suspend fun run() {
          logger.info{"${name}: Starting spider"}
          super.run()
          scope.launch {
               logger.info{"${name}: Start performing scraping"}
               performScraping(this)
               logger.info{"${name}: finished performing scraping"}
               itemsOut.send(ItemEnd())
          }
     }


     override suspend fun performResponse(response: Response) {
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
               val itemStart = ItemStartTaskProgress(taskSlot, taskMessage, nbSteps, slotMode)
               itemsOut.send(itemStart)
               val result = func(task)
               return result
          }
          catch(ex: Exception){
               if(optional){
                    logger.error(ex){"${ex.message}"}
                    val error = ItemError(ex, this, ErrorLevel.MINOR)
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
                    val itemStart = ItemStartStepProgress(slotId, stepMessage)
                    itemsOut.send(itemStart)
                    func(scrapingStep)
               }
               catch(ex: Exception){
                    val message = "in task ${slotId}-${taskName}, step ${stepMessage}: ${ex.message}"
                    val newEx = SpiderStepException(message, ex)
                    if(optional){
                         logger.error(newEx){"${name}: ${message}"}
                         val error = ItemError(newEx, this@AbstractSpider, ErrorLevel.MINOR)
                         itemsOut.send(error)
                    }
                    else{
                         throw newEx
                    }
               }
               finally {
                    val itemDone = ItemStepDoneProgress(slotId, step)
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
                    val error = ItemError(ex, this@AbstractSpider, ErrorLevel.MINOR)
                    itemsOut.send(error)
               }
          }



          suspend fun sendData(data: Data, label: String = "data"){
               val item = DataItem(data, label)
               itemsOut.send(item)
          }

     }

     inner class ScrapingStep(){
          suspend fun sendData(data: Data, label: String = "data"){
               val item = DataItem(data, label)
               itemsOut.send(item)
          }

          suspend fun sendUpdate(update: ItemUpdate){
               itemsOut.send(update)
          }
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
               parse(resp)
          }
          catch(ex: Throwable){
               callbackError(ex)
          }
     }

     abstract suspend fun parse(resp: Response)

     abstract suspend fun callbackError(ex: Throwable)
}