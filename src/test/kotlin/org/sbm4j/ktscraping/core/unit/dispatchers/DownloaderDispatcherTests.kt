package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.components.logger
import org.sbm4j.ktscraping.core.dispatchers.DownloaderDispatcher
import org.sbm4j.ktscraping.core.dispatchers.SendPropagator
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.core.utils.generateRequestResponse
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import kotlin.test.Test

class TestingDownloaderDispatcher(override val di: DI): DownloaderDispatcher(di = di){
    companion object{
        const val indexKey = "numDownloader"
    }

    override fun selectChannel(request: AbstractRequest): SuperChannel {
        val key = request.parameters[indexKey] as Int
        return this.receivers[key]
    }

}



class DownloaderRequestDispatcherTests : AbstractSendDispatcherTester() {

    override fun buildDispatcher(): SendPropagator {
        return TestingDownloaderDispatcher(di = di)
    }


    @Test
    fun testSendRequest() = TestScope().runTest {
        val (req1, resp1) = generateRequestResponse(sender)
        req1.parameters[TestingDownloaderDispatcher.indexKey] = 1

        withDispatcher(listOf(0, 1, 0)){
            val response = inChannel.sendSync<DownloadingResponse>(req1)
            logger.debug{"Received response: $response"}
        }
    }
}