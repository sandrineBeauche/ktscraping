package org.sbm4j.ktscraping.core.utils

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.core.dispatchers.SendPropagator
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.components.SendSource
import kotlin.collections.forEach
import kotlin.test.BeforeTest

interface SendDispatcherTester{
    var inChannel: SuperChannel

    var outChannels: List<SuperChannel>

    var sender: SendSource

    val di: DI

    var dispatcher: SendPropagator

    val nbBranches: Int

    @BeforeTest
    fun setUp(){
        dispatcher = buildDispatcher()

        inChannel = SuperChannel()
        outChannels = List(nbBranches){ SuperChannel() }

        dispatcher.channelIn = inChannel
        dispatcher.receivers.addAll(outChannels)

        sender = mockk<SendSource>()
    }

    fun buildDispatcher(): SendPropagator

    suspend fun doStartEvent(){
        logger.debug{"Do Start event"}
        val startEvent = StartEvent(sender)
        val result = inChannel.sendSync<EventBack>(startEvent)
        logger.debug{"Start event done: $result"}
    }

    suspend fun doEndEvent(){
        logger.debug{"Do End event"}
        val endEvent = EndEvent(sender)
        val result = inChannel.sendSync<EventBack>(endEvent)
        logger.debug{"End event done: $result"}
    }

    suspend fun initChannels(parentScope: CoroutineScope){
        inChannel.init(parentScope)
        outChannels.forEach {it.init(parentScope)}
    }

    suspend fun closeChannels(){
        logger.debug{"Close channels"}
        inChannel.close()
        outChannels.forEach {it.close()}
        logger.debug { "Close channels finished" }
    }

    fun launchBranchesStubs(scope: CoroutineScope, nbMessages: List<Int> = emptyList())

    suspend fun withDispatcher(nbMessages: List<Int>, func: suspend SendDispatcherTester.() -> Unit) {
        coroutineScope {
            initChannels(this)

            launch {
                dispatcher.start(this)
                doStartEvent()
                func()
                doEndEvent()
                dispatcher.stop()

                closeChannels()
            }
            launchBranchesStubs(this, nbMessages)
        }
    }
}

abstract class AbstractSendDispatcherTester: SendDispatcherTester{

    override lateinit var inChannel: SuperChannel

    override lateinit var outChannels: List<SuperChannel>

    override lateinit var sender: SendSource

    override val di: DI = mockk<DI>()

    override lateinit var dispatcher: SendPropagator

    override val nbBranches: Int = 3

    override fun launchBranchesStubs(
        scope: CoroutineScope,
        nbMessages: List<Int>
    ) {
        nbMessages.forEachIndexed { index, nb ->
            scope.launch {
                val ch = outChannels[index]
                ch.getSendFlow().take(nb + 2).collect { send ->
                    logger.debug { "branch #$index: received $send and answers with a back" }
                    val back = send.buildBack()
                    ch.send(back)
                }
                logger.debug { "Receiver #$index: Finished with $nb messages" }
            }
        }
    }
}

