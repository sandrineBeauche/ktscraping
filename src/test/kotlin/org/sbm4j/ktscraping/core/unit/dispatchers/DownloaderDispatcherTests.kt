package org.sbm4j.ktscraping.core.unit.dispatchers

import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.sbm4j.ktscraping.core.dispatchers.DownloaderDispatcher
import org.sbm4j.ktscraping.core.utils.AbstractSendDispatcherTester
import org.sbm4j.ktscraping.core.utils.generateRequestResponse
import org.sbm4j.ktscraping.data.events.EventBack
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.nodes.logger
import kotlin.test.Test

class TestingDownloaderDispatcher(override val di: DI): DownloaderDispatcher(di = di){
    companion object{
        const val indexKey = "numDownloader"
    }

    override fun selectChannel(request: AbstractRequest): SuperChannel {
        val key = request.parameters[indexKey] as Int
        return this.channelOuts[key]
    }
}


class DownloaderRequestDispatcherTests :
    AbstractSendDispatcherTester<TestingDownloaderDispatcher>()
{
    override val nbChannelsOuts: Int = 3

    override fun buildNode(): TestingDownloaderDispatcher {
        val result = TestingDownloaderDispatcher(di = di)
        result.channelIn = this.channelIn
        result.channelOuts.addAll(channelOuts)
        return result
    }


    @Test
    fun `send request`() = testScope.runTest {
        val (req1, resp1) = generateRequestResponse(sender)
        req1.parameters[TestingDownloaderDispatcher.indexKey] = 1

        val response = channelIn.sendSync<DownloadingResponse>(req1)
        logger.debug{"Received response: $response"}

        verifyNbInvocations(listOf(0, 1, 0))
    }

    @Test
    fun `send event`() = testScope.runTest {
        val event = StartEvent(sender)

        val response = channelIn.sendSync<EventBack>(event)
        logger.debug{"Received response: $response"}

        verifyNbInvocations(listOf(1, 1, 1))
    }
}