package org.sbm4j.ktscraping.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.sbm4j.ktscraping.core.Controllable
import org.sbm4j.ktscraping.core.CrawlerConfiguration
import org.sbm4j.ktscraping.core.State
import org.sbm4j.ktscraping.core.logger
import org.sbm4j.ktscraping.requests.AbstractRequest


class Scheduler(override val scope: CoroutineScope, val configuration: CrawlerConfiguration): Controllable {

    lateinit var requestOut: SendChannel<AbstractRequest>

    override val mutex: Mutex = Mutex()
    override val name: String = "Scheduler"
    override var state: State = State()

    val requestSemaphore: Semaphore = Semaphore(configuration.nbConnexions, 0)

    val pendingRequest: MutableMap<String, Channel<AbstractRequest>> = mutableMapOf()

    suspend fun submitRequest(request: AbstractRequest) {
        val server = request.extractServerFromUrl()
        var ch: Channel<AbstractRequest>
        mutex.withLock {
            logger.debug { "${name}: Scheduling request ${request.name} on server ${server}" }
            if(!pendingRequest.containsKey(server)){
                ch = Channel(Channel.UNLIMITED)
                pendingRequest.put(server, ch)
                scope.launch {
                    for(req in ch){
                        requestSemaphore.acquire()
                        logger.debug { "${name}: Sending request ${req.name}" }
                        requestOut.send(req)
                        if(configuration.autoThrottle > 0){
                            logger.debug { "${name}: delai of ${configuration.autoThrottle}ms for auto-throttle"}
                            delay(10000)
                            logger.debug { "${name}: ready to send another request" }
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
        logger.debug{"${name}: start scheduling requests"}
    }

    override suspend fun stop() {
        super.stop()
        logger.debug{"${name}: stop scheduling requests"}
        for(ch in pendingRequest.values){
            ch.close()
        }
    }

    fun receivedResponse(){
        requestSemaphore.release()
    }

}