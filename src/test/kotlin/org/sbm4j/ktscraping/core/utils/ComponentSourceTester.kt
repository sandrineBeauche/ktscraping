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

class ComponentStub(name: String, inChannel: SuperChannel) : Stub(name, inChannel){

    val downloadingResponses: MutableMap<String, Pair<ContentType, MutableMap<String, Any>>> =
        mutableMapOf()

    suspend fun performRequest(request: AbstractRequest): Any {
        val result = processSend(request)
        if(result is DownloadingResponse){
            val contents =
                downloadingResponses[request.toURIString()]
            if(contents != null){
                result.type = contents.first
                result.contents.putAll(contents.second)
            }
        }
        return result
    }

    suspend fun processInternal(internal: Internal): Any? {
        return null
    }

    suspend fun processEvent(event: Event): Any?{
        return processSend(event)
    }

    suspend fun processItem(item: Item): Any?{
        return processSend(item)
    }

    override suspend fun sendPostProcess(send: Send, result: Any) {
        if(send !is Internal){
            super.sendPostProcess(send, result)
        }
    }

    override suspend fun run() {
        val requestClazz = AbstractRequest::class
        val flowRequest = inChannel.getSendFlow(requestClazz)
        performSends(requestClazz, flowRequest, ::performRequest)

        val itemClazz = Item::class
        val flowItem = inChannel.getSendFlow(itemClazz)
        performSends(itemClazz, flowItem, ::processItem)

        val eventClazz = Event::class
        val flowEvent = inChannel.getSendFlow(eventClazz)
        performSends(eventClazz, flowEvent, ::processEvent)

        val  internalClazz = Internal::class
        val flowInternal = inChannel.getSendFlow(internalClazz)
        performSends(internalClazz, flowInternal, ::processInternal)

        inChannel.awaitReady()
    }

    override suspend fun stop() {
        downloadingResponses.clear()
        super.stop()
    }


}

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