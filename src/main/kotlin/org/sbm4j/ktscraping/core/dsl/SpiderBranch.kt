package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.channels.Channel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.requests.AbstractRequest
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Response

fun buildSpiderChannels(): Triple<Channel<Response>, Channel<AbstractRequest>, Channel<Item>>{
    return Triple(
        Channel<Response>(Channel.UNLIMITED),
        Channel<AbstractRequest>(Channel.UNLIMITED),
        Channel<Item>(Channel.UNLIMITED)
    )
}

fun Crawler.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val branch = SpiderBranch(
        this.channelFactory.spiderRequestChannel,
        this.channelFactory.spiderResponseChannel,
        this.channelFactory.spiderItemChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.spiderDispatcher(name: String = "dispatcher", initDispatcher: SpiderResponseDispatcher.() -> Unit){
    val dispatcher = SpiderResponseDispatcher(name, this.di)
    dispatcher.channelOut = this.channelFactory.spiderRequestChannel
    dispatcher.channelIn = this.channelFactory.spiderResponseChannel
    dispatcher.itemChannelOut = this.channelFactory.spiderItemChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}



class SpiderBranch(
    var spiderIn: Channel<AbstractRequest>,
    var spiderOut: Channel<Response>,
    var spiderItemIn: Channel<Item>,
    override val di: DI
) : DIAware {

    val senders : MutableList<Controllable> = mutableListOf()

    inline fun <reified T : SpiderMiddleware>spiderMiddleware(
                                               name: String? = null,
                                               init: T.() -> Unit = {}): T{
        val mid = buildControllable<T>(name)

        senders.add(mid)
        mid.requestOut = spiderIn
        mid.responseIn = spiderOut
        mid.itemOut = spiderItemIn

        val (spidResp, spidReq, spidItem) = buildSpiderChannels()
        mid.requestIn = spidReq
        mid.responseOut = spidResp
        mid.itemIn = spidItem

        spiderIn = spidReq
        spiderOut = spidResp
        spiderItemIn = spidItem

        mid.init()

        return mid
    }

    inline fun <reified T: AbstractSpider>spider(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val spid = buildControllable<T>(name)

        senders.add(spid)
        spid.requestOut = spiderIn
        spid.responseIn = spiderOut
        spid.itemsOut = spiderItemIn

        spid.init()

        return spid
    }

    fun spiderDispatcher(name: String = "dispatcher", init: SpiderResponseDispatcher.() -> Unit){
        val dispatcher = SpiderResponseDispatcher(name, this.di)
        dispatcher.channelOut = spiderIn
        dispatcher.channelIn = spiderOut
        dispatcher.itemChannelOut = spiderItemIn
        dispatcher.init()
        senders.add(dispatcher)
    }

}

inline fun <reified T: AbstractSpider> SpiderResponseDispatcher.spider(
    name: String? = null,
    init: T.() -> Unit = {}
): T {
    val spid = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(spid)

    val (spidResp, spidReq, spidItem) = buildSpiderChannels()
    spid.requestOut = spidReq
    spid.responseIn = spidResp
    val itemChannel = spidItem
    spid.itemsOut = itemChannel

    this.addBranch(spidReq, spidResp, itemChannel)
    spid.init()

    return spid
}

fun SpiderResponseDispatcher.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val (spidResp, spidReq, spidItem) = buildSpiderChannels()
    this.addBranch(spidReq, spidResp, spidItem)
    val branch = SpiderBranch(spidReq, spidResp, spidItem, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}