package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.core.DownloaderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Response

class SchedulerMiddleware(name: String = "Scheduler middleware"): DownloaderMiddleware(name) {

    var nbConnexions: Int = 10
        set(value) {
            requestSemaphore = Semaphore(value, 0)
        }

    var autoThrottle: Int = 2000

    var requestSemaphore: Semaphore = Semaphore(nbConnexions, 0)

    val pendingRequest: MutableMap<String, Channel<AbstractRequest>> = mutableMapOf()

    override suspend fun processResponse(response: Response): Boolean {
        requestSemaphore.release()
        return true
    }

    override suspend fun processRequest(request: AbstractRequest): Any? {
        return true
    }

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        submitRequest(request)
    }

    private fun createServerChannel(server: String): Channel<AbstractRequest>{
        val ch = Channel<AbstractRequest>(Channel.UNLIMITED)
        pendingRequest[server] = ch
        scope.launch {
            for(req in ch){
                requestSemaphore.acquire()
                logger.debug { "${name}: Sending request ${req.name}" }
                requestOut.send(req)
                if(autoThrottle > 0){
                    logger.debug { "${name}: delai of ${autoThrottle}ms for auto-throttle"}
                    delay(autoThrottle.toLong())
                    logger.debug { "${name}: ready to send another request" }
                }
            }
        }
        return ch
    }

    suspend fun submitRequest(request: AbstractRequest) {
        val server = request.extractServerFromUrl()
        var ch: Channel<AbstractRequest>
        mutex.withLock {
            logger.debug { "${name}: Scheduling request ${request.name} on server ${server}" }
            ch = if(!pendingRequest.containsKey(server)){
                createServerChannel(server)
            }
            else{
                pendingRequest[server]!!
            }
        }
        ch.send(request)
    }


    override suspend fun run() {
        logger.info{"${name}: Starting scheduler middleware"}
        super.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping scheduler middleware"}
        this.pendingRequest.values.forEach {
            it.close()
        }
        super.stop()
    }
}