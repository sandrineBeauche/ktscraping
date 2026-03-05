package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.dispatchers.DownloaderDispatcher
import org.sbm4j.ktscraping.data.request.AbstractRequest


fun Crawler.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val branch = DownloaderBranch(
        this.channelManager.downloaderChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}


fun Crawler.downloaderDispatcher(
    name: String = "dispatcher",
    selectChannelFunc: DownloaderDispatcher.(AbstractRequest) -> SuperChannel,
    initDispatcher: DownloaderDispatcher.() -> Unit
){
    val dispatcher = object : DownloaderDispatcher(name, this.di){
        override fun selectChannel(request: AbstractRequest): SuperChannel {
            return selectChannelFunc(request)
        }
    }

    dispatcher.channelIn = this.channelManager.downloaderChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


class DownloaderBranch(
    var downloaderChannel: SuperChannel,
    override val di: DI
): DIAware{

    val senders: MutableList<Controllable> = mutableListOf()

    inline fun <reified T: AbstractMiddleware>middleware(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val mid = buildControllable<T>(name)

        senders.add(mid)
        mid.inChannel = downloaderChannel

        val newChannel = SuperChannel()
        mid.outChannel = newChannel

        downloaderChannel = newChannel

        mid.init()
        return mid
    }

    inline fun <reified T: AbstractDownloader>downloader(
                                          name: String? = null,
                                          init: T.() -> Unit = {}): T{
        val down = buildControllable<T>(name)
        senders.add(down)

        down.inChannel = downloaderChannel
        down.init()

        return down
    }

    fun downloaderDispatcher(
        name: String = "dispatcher",
        selectChannelFunc: DownloaderDispatcher.(AbstractRequest) -> SuperChannel,
        init: DownloaderDispatcher.() -> Unit
    ){
        val dispatcher = object : DownloaderDispatcher(name, this.di){
            override fun selectChannel(request: AbstractRequest): SuperChannel {
                return selectChannelFunc(request)
            }
        }

        dispatcher.channelIn = downloaderChannel
        dispatcher.init()
        senders.add(dispatcher)
    }
}

inline fun <reified T: AbstractDownloader> DownloaderDispatcher.downloader(
                                                                   name: String? = null,
                                                                   init: T.() -> Unit = {}): T {
    val down = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(down)

    val channel = SuperChannel()
    down.inChannel = channel

    this.addBranch(channel)
    down.init()

    return down
}

fun DownloaderDispatcher.downloaderBranch(initBranch: DownloaderBranch.() -> Unit){
    val channel = SuperChannel()
    this.addBranch(channel)
    val branch = DownloaderBranch(channel, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}