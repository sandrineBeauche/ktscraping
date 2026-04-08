package org.sbm4j.ktscraping.core.utils

import io.mockk.coVerify
import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.internal.Internal
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.meercat.Stub
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.SourceNodeTester
import org.sbm4j.meercat.nodes.sendProcessors.SendSource


interface ComponentSourceTester<T: SendSource>: SourceNodeTester<T> {

    override fun buildStub(channel: SuperChannel): Stub {
        return ComponentStub("componentStub", channel)
    }

    fun getReceivedRequest(): List<AbstractRequest>{
        val result = mutableListOf<AbstractRequest>()
        coVerify { (stub as ComponentStub).performRequest(capture(result)) }
        return result
    }

    fun getReceivedEvent(): List<Event>{
        val result = mutableListOf<Event>()
        coVerify { (stub as ComponentStub).processEvent(capture(result)) }
        return result
    }

    fun getReceivedItem(): List<Item>{
        val result = mutableListOf<Item>()
        coVerify { (stub as ComponentStub).processItem(capture(result)) }
        return result
    }

    fun getReceivedInternal(): List<Internal>{
        val result = mutableListOf<Internal>()
        coVerify { (stub as ComponentStub).processInternal(capture(result)) }
        return result
    }

}