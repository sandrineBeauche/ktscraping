package org.sbm4j.ktscraping.core.dsl

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.kodein.di.*
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.requests.Data


data class DataItemTest(
    val value: String,
    val reqName: String,
    val url: String = "une url",
): Data(){
    override fun clone(): Data {
        val result = this.copy()
        return result
    }

}


class EmptyTestingCrawler(
    scope: CoroutineScope,
    name: String = "TestCrawler",
    channelFactory: ChannelFactory,
    override val di: DI
) : AbstractCrawler(scope, name, channelFactory){
    override suspend fun start() {
        logger.info{"Starting testing crawler ${name}"}
        super.start()
    }

    override suspend fun stop() {
        logger.info{"Stopping testing crawler ${name}"}
        super.stop()
    }

    override suspend fun waitFinished(): CrawlerResult {
        TODO("Not yet implemented")
    }
}


abstract class CrawlerTest {

    val scope = TestScope()

    val sender: RequestSender = mockk<RequestSender>()

    val channelFactory : ChannelFactory = ChannelFactory()

    fun testDIModule(scope: CoroutineScope, name: String): DI.Module {
        val mod = DI.Module(name = "testDIModule"){
            bindSingleton<CoroutineScope> { scope }
            bind<Crawler> { multiton { di: DI -> EmptyTestingCrawler(instance(), name, instance(), di) }}
            bindSingleton<ChannelFactory> { channelFactory }
        }
        return mod
    }
}