package org.sbm4j.ktscraping.core.processors

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.EventPropagation
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.data.Back
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.data.ErrorLevel

import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.nodes.BackForwarder
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendConsumer

import java.util.concurrent.ConcurrentHashMap

/**
 * A [Deferred] result of an asynchronous pre-event job, carrying the final [Status]
 * and any [ErrorInfo]s accumulated during execution.
 *
 * Pre-event jobs are launched during [EventProcessor.performEvent] and awaited later
 * when the corresponding [EventBack] is received.
 *
 * @see EventProcessor
 * @see EventConsumer
 * @see EventBackForwarder
 */
typealias EventJobResult = Deferred<Pair<Status, List<ErrorInfo>>>

/**
 * Defines the event processing lifecycle for nodes that handle [Event] and [EventBack] messages.
 *
 * Provides hooks for pre-event processing (before the event is forwarded downstream)
 * and post-event processing (when the corresponding [EventBack] is received).
 * Default implementations are no-ops, allowing implementors to override only the hooks
 * they need.
 *
 * Pre-event hooks ([preStart], [preEnd], [preCustomEvent]) may return an [EventJobResult]
 * to perform asynchronous work (e.g. opening a connection, initializing a resource).
 * The result is awaited and merged into the [EventBack] by [EventBackForwarder.resumeEvent].
 *
 * @see EventConsumer
 * @see EventBackForwarder
 * @see EventSink
 */
interface EventProcessor{

    /**
     * Dispatches the incoming [event] to the appropriate pre-event hook.
     *
     * Routes [StartEvent] to [preStart], [EndEvent] to [preEnd],
     * and any other event to [preCustomEvent].
     *
     * Each hook can choose between two processing modes:
     * - **Synchronous**: perform the work directly in the hook and return `null`.
     *   Use this when the processing is immediate (e.g. sending downstream messages).
     * - **Asynchronous**: launch a coroutine and return an [EventJobResult].
     *   The result will be awaited by [EventBackForwarder.resumeEvent] when the
     *   corresponding [EventBack] is received.
     *
     * @param event The incoming event to process.
     * @return An [EventJobResult] if asynchronous work was started, `null` for synchronous processing.
     */
    suspend fun performEvent(event: Event): Any?{
        return when(event){
            is StartEvent -> preStart(event)
            is EndEvent -> preEnd(event)
            else -> preCustomEvent(event)
        }
    }

    /**
     * Pre-event hook called when a [StartEvent] is received.
     *
     * Override to perform initialization work (e.g. opening connections, loading config,
     * sending downstream messages). Return `null` for synchronous processing, or an
     * [EventJobResult] if the work should be completed asynchronously before the back is forwarded.
     *
     * @return An [EventJobResult] for asynchronous mode, `null` for synchronous mode.
     */
    suspend fun preStart(event: Event): EventJobResult?{
        return null
    }

    /**
     * Pre-event hook called when an [EndEvent] is received.
     *
     * Override to perform teardown work (e.g. flushing buffers, closing connections,
     * sending downstream messages). Return `null` for synchronous processing, or an
     * [EventJobResult] if the work should be completed asynchronously before the back is forwarded.
     *
     * @return An [EventJobResult] for asynchronous mode, `null` for synchronous mode.
     */
    suspend fun preEnd(event: Event): EventJobResult?{
        return null
    }

    /**
     * Pre-event hook called when any non-standard event is received.
     *
     * Override to handle custom application-specific events. Return `null` for synchronous
     * processing, or an [EventJobResult] if the work should be completed asynchronously
     * before the back is forwarded.
     *
     * @return An [EventJobResult] for asynchronous mode, `null` for synchronous mode.
     */
    suspend fun preCustomEvent(event: Event): Any?{
        return true


    }

    /**
     * Dispatches the incoming [EventBack] to the appropriate post-event hook.
     *
     * Routes backs for `"start"` to [postStart], `"end"` to [postEnd],
     * and any other event name to [postCustomEvent].
     *
     * @param event The incoming event back to process.
     */
    suspend fun performPostEvent(event: EventBack){
        when(event.send.eventName){
            "start" -> postStart(event)
            "end" -> postEnd(event)
            else -> postCustomEvent(event)
        }
    }

    /**
     * Post-event hook called when the back for a [StartEvent] is received.
     * Override to react after all downstream nodes have completed their initialization.
     */
    suspend fun postStart(event: EventBack){}

    /**
     * Post-event hook called when the back for an [EndEvent] is received.
     * Override to react after all downstream nodes have completed their teardown.
     */
    suspend fun postEnd(event: EventBack){}

    /**
     * Post-event hook called when the back for a custom event is received.
     * Override to react after all downstream nodes have processed a custom event.
     */
    suspend fun postCustomEvent(event: EventBack){}
}

/**
 * A [SendConsumer] that processes [Event] messages from the Spider branch.
 *
 * Combines [SendConsumer] and [EventProcessor] to handle events flowing through
 * an intermediate node. When an event is received, [consumeEvent] calls [performEvent]
 * and stores the resulting [EventJobResult] (if any) in [pendingEventJobs], keyed by
 * [Event.eventName]. The job result is later awaited by the paired [EventBackForwarder]
 * when the corresponding [EventBack] is received.
 *
 * The [jobPreEvent] helper launches a coroutine for asynchronous pre-event work and
 * wraps any exception into an [ErrorInfo].
 *
 * @see EventBackForwarder
 * @see EventProcessor
 */
interface EventConsumer: SendConsumer, EventProcessor {

    /**
     * Map of pending asynchronous pre-event jobs, keyed by [Event.eventName].
     * Jobs are added by [consumeEvent] and removed by [EventBackForwarder.resumeEvent].
     */
    val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>

    /**
     * Receives an [Event], triggers the pre-event hook via [performEvent], and stores
     * the resulting job in [pendingEventJobs] if one was started.
     *
     * @param event The incoming event to consume.
     * @return The original [event], forwarded downstream.
     */
    suspend fun consumeEvent(event: Event): Any?{
        logger.trace{"${name}: received an event to consume: ${event}"}
        return when(val performResult = performEvent(event)) {
            is Deferred<*> -> {
                pendingEventJobs[event.eventName] = performResult as EventJobResult
                true
            }
            null -> true
            else -> performResult
        }
    }

    /**
     * Subscribes to the [Event] flow from [inChannel], optionally filtering by [propagation].
     *
     * @param propagation If non-null, only events with this propagation value are processed.
     */
    suspend fun registerEventListening(propagation: EventPropagation? = null){
        val clazz = Event::class
        val flow = inChannel.getSendFlow(clazz)
        val filtered = if(propagation != null)
            flow.filter({ it.propagation == propagation })
        else flow
        this.performSends(clazz, filtered, ::consumeEvent)
    }


    override suspend fun run() {
        registerEventListening()
    }

    /**
     * Launches an asynchronous pre-event job and wraps any exception into an [ErrorInfo].
     *
     * Use this helper inside [preStart], [preEnd], or [preCustomEvent] to perform
     * asynchronous work whose result should be merged into the [EventBack].
     *
     * @param defaultErrorLevel Severity level to use if an exception is caught, defaults to [ErrorLevel.MAJOR].
     * @param errorMessage Optional message to include in the [ErrorInfo] if an exception is caught.
     * @param func The asynchronous work to perform.
     * @return An [EventJobResult] tracking the outcome of [func].
     */
    suspend fun jobPreEvent(
        defaultErrorLevel: ErrorLevel = ErrorLevel.MAJOR,
        errorMessage: String = "",
        func: suspend () -> Unit
    ): EventJobResult? {
        return scope.async {
            try {
                func()
                Pair(Status.OK, listOf())
            } catch (e: Exception) {
                val info = generateErrorInfos(e, defaultErrorLevel, errorMessage)
                Pair(Status.ERROR, listOf(info))
            }
        }
    }
}


/**
 * A [BackForwarder] that awaits pending [EventJobResult]s and triggers post-event hooks
 * when [EventBack] messages are received.
 *
 * Works in tandem with [EventConsumer]: when an [EventBack] is received, [resumeEvent]
 * retrieves the corresponding job from [pendingEventJobs], awaits its result, merges
 * the status and errors into the back, then calls [performPostEvent] to trigger the
 * appropriate post-event hook before forwarding the back upstream.
 *
 * @see EventConsumer
 * @see EventProcessor
 */
interface EventBackForwarder: BackForwarder, EventProcessor {

    /**
     * Map of pending asynchronous pre-event jobs, keyed by [Event.eventName].
     * Shared with the paired [EventConsumer] — jobs are added there and removed here.
     */
    val pendingEventJobs: ConcurrentHashMap<String, EventJobResult>

    /**
     * Awaits the pending job for the received [EventBack], merges its result into the back,
     * then triggers the appropriate post-event hook via [performPostEvent].
     *
     * Any exception thrown during job awaiting is caught and added to [EventBack.errorInfos].
     *
     * @param event The incoming event back to process.
     */
    suspend fun resumeEvent(event: EventBack){
        try {
            val eventName = event.send.eventName
            val job = pendingEventJobs.remove(eventName)
            val result = job?.await()
            if(job != null && result != null) {
                event.status += result.first
                event.errorInfos.addAll(result.second)
            }
            performPostEvent(event)
        }
        catch(ex: Exception){
            val infos = generateErrorInfos(ex)
            event.errorInfos.add(infos)
        }
    }

    /**
     * Subscribes to the [EventBack] flow from [outChannel], optionally filtering by [propagation].
     *
     * @param propagation If non-null, only backs for events with this propagation value are processed.
     */
    suspend fun registerEventBackListening(propagation: EventPropagation? = null){
        val flow = this@EventBackForwarder.outChannel.getBackFlow(EventBack::class, this)
        val filtered = if(propagation != null)
            flow.filter { it.send.propagation == propagation }
        else flow
        receiveBacks(EventBack::class, filtered, ::resumeEvent)
    }

    override suspend fun run() {
        registerEventBackListening()
    }
}

/**
 * An [EventConsumer] for terminal nodes (sinks) in the topology.
 *
 * Unlike [EventConsumer] which stores the job result for later retrieval by an
 * [EventBackForwarder], [EventSink] awaits the pre-event job immediately and builds
 * the [EventBack] itself before calling the post-event hook. This is appropriate for
 * nodes at the end of a branch that have no downstream node to forward backs to.
 *
 * @see EventConsumer
 * @see EventProcessor
 */
interface EventSink: EventConsumer{

    /**
     * Processes the [event] synchronously: awaits the pre-event job, builds the back,
     * and triggers the post-event hook via [performPostEvent].
     *
     * Any exception is caught and reflected in the returned [EventBack].
     *
     * @param event The incoming event to consume.
     * @return An [EventBack] reflecting the outcome of the pre-event job.
     */
    override suspend fun consumeEvent(event: Event): Any? {
        lateinit var result: EventBack
        try {
            when(val r = performEvent(event)){
                is Deferred<*> -> {
                    val deferredResult = r.await() as Pair<Status, List<ErrorInfo>>?
                    result = event.buildBack()
                    result.status += deferredResult!!.first
                    result.errorInfos.addAll(deferredResult.second)
                }
                is Event, true, null -> result = event.buildBack()
                is EventBack -> result = r
            }
        }
        catch(ex: Exception){
            val error = this.generateErrorInfos(ex)
            result = event.buildErrorBack(error)
        }
        finally {
            performPostEvent(result)
        }
        return result
    }
}