package org.sbm4j.ktscraping.core.utils

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.meercat.dispatchers.PropagatorTester
import org.sbm4j.meercat.nodes.dispatchers.Propagator
import org.sbm4j.meercat.nodes.logger
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.collections.forEach
import kotlin.test.BeforeTest


abstract class AbstractSendDispatcherTester<T: Propagator>:
    PropagatorTester<T>()
{

    val di: DI = mockk<DI>()

    fun verifyNbInvocations(calls: List<Int>){
        calls.forEachIndexed { index, i ->
            coVerify(exactly = i) { stubs[index].processSend(any()) }
        }
    }

}

