package org.sbm4j.ktscraping.downloaders.playwright

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.Test

class PlaywrightThreadPoolTests {

    @Test
    fun testPlaywrightThreadPool() = TestScope().runTest {
        val factory = PlaywrightThreadfactory(true)

        val fixedThreadPool = Executors.newCachedThreadPool(factory).asCoroutineDispatcher()

        repeat(4) {
            launch(fixedThreadPool) {
                println("Running on fixed thread pool: ${Thread.currentThread().name}")
                val th = (Thread.currentThread() as PlaywrightThread)
                println("${th.name}: start the runnable")
                val page = th.context.newPage()
                Thread.sleep(500)
                page.navigate("http://www.google.fr")
                println("coucou ${th.name}")
                page.close()
            }
        }

        fixedThreadPool.close()
    }

}