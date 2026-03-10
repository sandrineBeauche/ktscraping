package org.sbm4j.ktscraping.core.utils

import io.mockk.clearAllMocks
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.meercat.channels.Status
import org.sbm4j.ktscraping.data.events.EndEvent
import org.sbm4j.ktscraping.data.events.Event
import org.sbm4j.ktscraping.data.events.StartEvent
import org.sbm4j.ktscraping.data.internal.ErrorInfo
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import kotlin.test.BeforeTest

abstract class AbstractMiddlewareTester: DualScrapingTest() {
    companion object{
        val RESP_CONTENTS = "Response_contents"
        val RESP_STATUS = "Response_status"
        val RESP_ERROR = "Response_error"
    }

    lateinit var middleware: AbstractMiddleware

    val middlewareName: String = "Middleware"


    abstract fun buildMiddleware(middlewareName: String): AbstractMiddleware

    @BeforeTest
    fun setUp(){
        buildChannels()
        clearAllMocks()

        middleware = buildMiddleware(middlewareName)

        middleware.inChannel = inChannel
        middleware.outChannel = outChannel
    }


    suspend fun processDownloadingRequest(request: DownloadingRequest){
        val resp = when(val status = request.parameters[RESP_STATUS] as Status){
            Status.OK -> {
                val result = request.buildBack()
                if(request.parameters.contains(RESP_CONTENTS)) {
                    val contents = request.parameters[RESP_CONTENTS] as MutableMap<String, Any>
                    result.contents.putAll(contents)
                }
                result
            }
            Status.ERROR, Status.FAIL -> {
                val error = request.parameters[RESP_ERROR] as ErrorInfo
                val result = request.buildErrorBack(error)
                result
            }

            Status.IGNORED -> {
                val result = request.buildBack()
                result.status = Status.IGNORED
                result
            }
        }
        outChannel.send(resp)
    }


    suspend fun processRequest(request: AbstractRequest){
        processSend(request)
    }


    suspend fun withMiddleware(nbMessages: Int = 1, func: suspend AbstractMiddlewareTester.() -> Unit){
        coroutineScope {
            initChannels(this)

            launch{
                middleware.start(this).join()
                doStartEvent()
                func()
                doEndEvent()
                middleware.stop()
                closeChannels()
            }
            launch{
                outChannel.getSendFlow().take(nbMessages + 2).collect { send ->
                    when(send){
                        is StartEvent, is EndEvent -> {
                            val back = send.buildBack()
                            outChannel.send(back)
                        }
                        is Event -> {
                            processEvent(send)
                        }
                        is DownloadingRequest -> {
                            processDownloadingRequest(send)
                        }
                        is AbstractRequest -> {
                            processRequest(send)
                        }
                    }
                }
            }
        }
    }
}