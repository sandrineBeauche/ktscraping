package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.data.request.AbstractRequest
import org.sbm4j.ktscraping.data.request.DownloadingRequest
import org.sbm4j.ktscraping.data.response.DownloadingResponse
import org.sbm4j.ktscraping.data.response.Response

fun buildDownloaderChannels(): Pair<Channel<AbstractRequest>, Channel<Response<*>>>{
    return Pair(
        Channel(Channel.UNLIMITED),
        Channel(Channel.UNLIMITED),
    )
}

fun Crawler.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val branch = DownloaderBranch(
        this.channelFactory.downloaderResponseChannel,
        this.channelFactory.downloaderRequestChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}


fun Crawler.downloaderDispatcher(
    name: String = "dispatcher",
    selectChannelFunc: DownloaderRequestDispatcher.(DownloadingRequest) -> SendChannel<AbstractRequest>,
    initDispatcher: DownloaderRequestDispatcher.() -> Unit
){
    val dispatcher = object : DownloaderRequestDispatcher(name, this.di){
        override fun selectChannel(request: DownloadingRequest): SendChannel<AbstractRequest> {
            return selectChannelFunc(request)
        }
    }
    dispatcher.channelOut = this.channelFactory.downloaderResponseChannel
    dispatcher.channelIn = this.channelFactory.downloaderRequestChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


class DownloaderBranch(
    var downloaderIn: Channel<Response<*>>,
    var downloaderOut: Channel<AbstractRequest>,
    override val di: DI
): DIAware{

    val senders: MutableList<Controllable> = mutableListOf()

    inline fun <reified T: AbstractMiddleware>middleware(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val mid = buildControllable<T>(name)

        senders.add(mid)
        /*
        mid.requestIn = downloaderOut
        mid.responseOut = downloaderIn

        val (downReq, downResp) = buildDownloaderChannels()
        mid.requestOut = downReq
        mid.responseIn = downResp

        downloaderIn = downResp
        downloaderOut = downReq

         */

        mid.init()
        return mid
    }

    inline fun <reified T: AbstractDownloader>downloader(
                                          name: String? = null,
                                          init: T.() -> Unit = {}): T{
        val down = buildControllable<T>(name)
        senders.add(down)
        /*
        down.requestIn = downloaderOut
        down.responseOut = downloaderIn
        down.init()


         */
        return down
    }

    fun downloaderDispatcher(
        name: String = "dispatcher",
        selectChannelFunc: DownloaderRequestDispatcher.(AbstractRequest) -> SendChannel<AbstractRequest>,
        init: DownloaderRequestDispatcher.() -> Unit
    ){
        val dispatcher = object : DownloaderRequestDispatcher(name, this.di){
            override fun selectChannel(request: DownloadingRequest): SendChannel<AbstractRequest> {
                return selectChannelFunc(request)
            }
        }
        dispatcher.channelOut = downloaderIn
        dispatcher.channelIn = downloaderOut
        dispatcher.init()
        senders.add(dispatcher)
    }
}

inline fun <reified T: AbstractDownloader> DownloaderRequestDispatcher.downloader(
                                                                   name: String? = null,
                                                                   init: T.() -> Unit = {}): T {
    val down = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(down)

    val (downReq, downResp) = buildDownloaderChannels()
    /*
    down.requestIn = downReq
    down.responseOut = downResp


     */
    this.addBranch(downReq, downResp)
    down.init()

    return down
}

fun DownloaderRequestDispatcher.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val (downReq, downResp) = buildDownloaderChannels()
    this.addBranch(downReq, downResp)
    val branch = DownloaderBranch(downResp, downReq, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}