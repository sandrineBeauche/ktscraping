package org.sbm4j.ktscraping.core.utils

import io.mockk.coVerify
import io.mockk.mockk
import org.kodein.di.DI
import org.sbm4j.meercat.dispatchers.PropagatorTester
import org.sbm4j.meercat.nodes.dispatchers.Propagator


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

