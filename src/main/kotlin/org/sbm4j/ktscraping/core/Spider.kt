package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


abstract class AbstractSpider(override val scope: CoroutineScope,
                              override val name: String = "Spider"): RequestSender{

     override val mutex: Mutex = Mutex()
     override var state: State = State()
     override var pendingRequests: PendingRequestMap = PendingRequestMap()

     override lateinit var requestOut: SendChannel<AbstractRequest>
     override lateinit var responseIn: ReceiveChannel<Response>

     lateinit var itemsOut: SendChannel<Item>

     abstract suspend fun performScraping()


     override suspend fun start() {
          logger.info{"${name}: Starting spider"}
          super.start()
          scope.launch {
               logger.info{"${name}: Start performing scraping"}
               performScraping()
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


     protected suspend fun sendSync(request: AbstractRequest) = suspendCoroutine<Response> { continuation ->
          scope.launch(CoroutineName("${name}-${request.name}")) {
               this@AbstractSpider.peformSend(request, continuation::resume, continuation::resumeWithException)
          }
     }
}


abstract class AbstractSimpleSpider(
     scope: CoroutineScope,
     name: String = "Spider"
) : AbstractSpider(scope, name) {

     lateinit var urlRequest: String

     override suspend fun performScraping() {
          val req = Request(this, urlRequest)
          logger.info { "${name} sends a new request ${req.name}" }
          this.send(req, ::parse, ::callbackError)
     }

     abstract suspend fun parse(resp: Response)

     abstract suspend fun callbackError(ex: Throwable)
}