package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

fun childScope(parentScope: CoroutineScope, name: String = "child"): CoroutineScope{
    val job = Job(parentScope.coroutineContext[Job])
    val scope =
        CoroutineScope(parentScope.coroutineContext
                + job
                + CoroutineName(name)
        )
    return scope
}