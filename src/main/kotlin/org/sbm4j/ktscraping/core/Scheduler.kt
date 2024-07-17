package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.requests.Request


class Scheduler(override val scope: CoroutineScope, val configuration: CrawlerConfiguration): Controllable {

    lateinit var requestOut: SendChannel<Request>

    override val mutex: Mutex = Mutex()
    override val name: String = "Scheduler"
    override var state: State = State()

    val requestSemaphore: Semaphore = Semaphore(configuration.nbConnexions, 0)

    val pendingRequest: MutableMap<String, Channel<Request>> = mutableMapOf()

    suspend fun submitRequest(request: Request) {
        val server = request.extractServerFromUrl()
        var ch: Channel<Request>
        mutex.withLock {
            logger.debug { "Scheduling request ${request} on server ${server}" }
            if(!pendingRequest.containsKey(server)){
                ch = Channel(Channel.UNLIMITED)
                pendingRequest.put(server, ch)
                scope.launch {
                    for(req in ch){
                        requestSemaphore.acquire()
                        logger.debug { "Sending request ${req}" }
                        requestOut.send(req)
                        if(configuration.autoThrottle > 0){
                            logger.debug { "delai of ${configuration.autoThrottle}ms for auto-throttle"}
                            delay(configuration.autoThrottle.toLong())
                        }
                    }
                }
            }
            else{
                ch = pendingRequest[server]!!
            }
        }
        ch.send(request)

    }



    override suspend fun start() {
        logger.debug{"start scheduling requests"}
    }

    override suspend fun stop() {
        super.stop()
        logger.debug{"stop scheduling requests"}
        for(ch in pendingRequest.values){
            ch.close()
        }
    }

    fun receivedResponse(){
        requestSemaphore.release()
    }

}