package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response




abstract class AbstractSpider(override val scope: CoroutineScope,
                              override val name: String = "Spider"): RequestSender{

     override val mutex: Mutex = Mutex()
     override var state: State = State()
     override var pendingRequests: PendingRequestMap = PendingRequestMap()

     override lateinit var requestOut: SendChannel<Request>
     override lateinit var responseIn: ReceiveChannel<Response>

     lateinit var itemsOut: SendChannel<Item>

     lateinit var urlRequest: String

     suspend fun startRequests(){
          val req = Request(this, urlRequest)
          logger.info{"${name} sends a new request ${req.name}"}
          this.send(req, ::parse, ::callbackError)
     }

     abstract suspend fun parse(req: Request, resp: Response)

     abstract suspend fun callbackError(req: Request, resp: Response)

     override suspend fun start() {
          logger.info{"Starting spider ${name}"}
          super.start()
          this.startRequests()
     }


     override suspend fun performResponse(response: Response) {
          throw NoRequestSenderException("request ${response.request.name} is not correlated to a sender")
     }


     override suspend fun stop() {
          this.itemsOut.close()
          super.stop()
     }
}