package org.sbm4j.ktscraping.core

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
 * An object from the kt scraping line that can be started, stopped or paused.
 * The state of this object can be saved on paused, and is safely used thanks to a mutex.
 * @property mutex the mutex that allow to safely use the state
 * @property name the name of this objet in the kt scraping line
 * @property state the state of the object
 * @property scope the scope where the kt scraping line object is used
 * @author Sandrine Ben Mabrouk
 */
interface Controllable {

    val mutex: Mutex

    val name: String

    var state: State

    var scope: CoroutineScope

    /**
     * Starts the kt scraping line object
     */
    suspend fun start(scope: CoroutineScope){
        scope.launch {
            this@Controllable.scope = this
            run()
        }
    }

    suspend fun run()

    /**
     * Stops the kt scraping line object
     */
    suspend fun stop(){
        try {
            //this.scope.cancel()
        }
        catch (ex: Exception){
            logger.info{ "crawler scope cancelled" }
        }
    }

    /**
     * Pauses the kt scraping line object
     */
    suspend fun pause(){
    }

    /**
     * Resumes the kt scraping line object
     */
    suspend fun resume(){
    }
}
