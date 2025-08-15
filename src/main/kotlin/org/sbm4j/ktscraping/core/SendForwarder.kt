package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A piece of the kt scraping line to perform some operations on requests, and answer by
 * following them to the next piece. In the other side, Middleware processes responses
 * before following them to the previous piece.
 */
interface SendForwarder<T: Send> : Controllable, SendSource<T, Back<T>>, SendConsumer<T>{

    suspend fun sendPostProcess(send: AbstractRequest, result: Any) {
        if(result is DownloadingResponse){
            logger.trace { "${name}: returns a ${result.loggingLabel} for the ${send.loggingLabel} ${send.name}" }
            inChannel.send(result)
        }
        else {
            logger.trace { "${name}: forward ${send.loggingLabel} ${send.name}" }
            outChannel.send(send)
        }
    }

}


abstract class AbstractMiddleware(override val name: String): Controllable{
    override val mutex: Mutex = Mutex()
    override var state: State = State()

    lateinit var inChannel: SuperChannel

    lateinit var outChannel: SuperChannel

    override lateinit var scope: CoroutineScope

}
/**
 *
 */
abstract class SpiderMiddleware(
    name:String
): AbstractMiddleware(name), SendForwarder<Send> {


    val backClazz = Back::class

    override suspend fun performBack(back: Back<Send>) {
    }

    override suspend fun run() {
        logger.info{"${name}: Starting spider middleware"}
        this.receiveBacks(this.backClazz as KClass<Back<Send>>)
    }



    override suspend fun stop() {
        logger.info{"${name}: Stopping spider middleware"}
        super<AbstractMiddleware>.stop()

    }

}

abstract class DownloaderMiddleware(name: String) :
    AbstractMiddleware(name),
    SendForwarder<AbstractRequest>,
    EventConsumer {

    override suspend fun stop() {
        super<AbstractMiddleware>.stop()
    }
}
