package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.core.DownloaderMiddleware
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse

class SchedulerMiddleware(name: String = "Scheduler middleware"): DownloaderMiddleware(name) {

    var nbConnexions: Int = 10
        set(value) {
            requestSemaphore = Semaphore(value, 0)
        }

    var autoThrottle: Int = 2000

    var requestSemaphore: Semaphore = Semaphore(nbConnexions, 0)

    val pendingRequest: MutableMap<String, Channel<AbstractRequest>> = mutableMapOf()

    override suspend fun processDownloadingResponse(response: DownloadingResponse, request: DownloadingRequest): Boolean {
        requestSemaphore.release()
        return true
    }

    override suspend fun processDataRequest(request: DownloadingRequest): Any? {
        return true
    }

    override suspend fun answerRequest(request: AbstractRequest, result: Any) {
        submitRequest(request as DownloadingRequest)
    }

    private fun createServerChannel(server: String): Channel<AbstractRequest>{
        val ch = Channel<AbstractRequest>(Channel.UNLIMITED)
        pendingRequest[server] = ch
        scope.launch {
            for(req in ch){
                requestSemaphore.acquire()
                logger.trace { "${name}: Sending request ${req.name}" }
                requestOut.send(req)
                if(autoThrottle > 0){
                    logger.trace { "${name}: delai of ${autoThrottle}ms for auto-throttle"}
                    delay(autoThrottle.toLong())
                    logger.trace { "${name}: ready to send another request" }
                }
            }
        }
        return ch
    }

    suspend fun submitRequest(request: DownloadingRequest) {
        val server = request.extractServerFromUrl()
        var ch: Channel<AbstractRequest>
        mutex.withLock {
            logger.trace { "${name}: Scheduling request ${request.name} on server ${server}" }
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