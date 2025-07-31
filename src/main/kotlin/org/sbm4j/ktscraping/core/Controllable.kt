package org.sbm4j.ktscraping.core

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

val logger = KotlinLogging.logger {}

/**
 *
 */
typealias  State = ConcurrentHashMap<String, Any>

/**
 * A component of the kt scraping crawler that can be started, stopped or paused and resumed.
 * The state of this object can be saved on paused, and is safely used thanks to a mutex.
 * Each component has his own coroutine scope of execution, and has potentially subscopes
 * for each input channel in order to receive requests, responses or items.
 * @property mutex the mutex that allow to safely use the state
 * @property name the name of this objet in the kt scraping line
 * @property state the state of the object
 * @property scope the coroutine scope for this component.
 * @author Sandrine Ben Mabrouk
 */
interface Controllable {

    val mutex: Mutex

    val name: String

    var state: State

    var scope: CoroutineScope


    /**
     * Starts the kt scraping component. The component is then executed in a coroutine subscope of the given scope
     * @param scope the parent coroutine scope
     */
    suspend fun start(scope: CoroutineScope){
        scope.launch {
            try {
                this@Controllable.scope = this
                run()
            }
            catch(ex: CancellationException){
                logger.debug { "Cancellation exception" }
            }
        }
    }

    /**
     * Runs the component. This method should be responsible to launch subscope to listen
     * the input channels.
     */
    suspend fun run()

    /**
     * Stops the kt scraping component
     */
    suspend fun stop(){
        try {
            this.scope.cancel()
        }
        catch (ex: Exception){
            logger.info{ "crawler scope cancelled" }
        }
    }

    /**
     * Pauses the kt scraping component
     */
    suspend fun pause(){
    }

    /**
     * Resumes the kt scraping component
     */
    suspend fun resume(){
    }
}
