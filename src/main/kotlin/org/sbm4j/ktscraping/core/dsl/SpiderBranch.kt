package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.ktscraping.core.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.core.components.SpiderMiddleware
import org.sbm4j.ktscraping.core.dispatchers.SpiderDispatcher


fun Crawler.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val branch = SpiderBranch(
        this.channelFactory.spiderChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.spiderDispatcher(name: String = "dispatcher", initDispatcher: SpiderDispatcher.() -> Unit){
    val dispatcher = SpiderDispatcher(name, this.di)
    dispatcher.channelOut = this.channelFactory.spiderChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}



class SpiderBranch(
    var channel: SuperChannel,
    override val di: DI
) : DIAware {

    val senders : MutableList<Controllable> = mutableListOf()

    inline fun <reified T : SpiderMiddleware>spiderMiddleware(
                                               name: String? = null,
                                               init: T.() -> Unit = {}): T{
        val mid = buildControllable<T>(name)

        senders.add(mid)

        mid.outChannel = channel
        val newChannel = SuperChannel()
        mid.inChannel = newChannel
        channel = newChannel

        mid.init()

        return mid
    }

    inline fun <reified T: AbstractSpider>spider(
                                  name: String? = null,
                                  init: T.() -> Unit = {}): T {
        val spid = buildControllable<T>(name)

        senders.add(spid)
        spid.outChannel = channel

        spid.init()
        return spid
    }

    fun spiderDispatcher(name: String = "dispatcher", init: SpiderDispatcher.() -> Unit){
        val dispatcher = SpiderDispatcher(name, this.di)
        dispatcher.channelOut = channel
        dispatcher.init()
        senders.add(dispatcher)
    }

}

inline fun <reified T: AbstractSpider> SpiderDispatcher.spider(
    name: String? = null,
    init: T.() -> Unit = {}
): T {
    val spid = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(spid)

    val newChannel = SuperChannel()
    spid.outChannel = newChannel
    this.addBranch(newChannel)
    spid.init()

    return spid
}

fun SpiderDispatcher.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val channel = SuperChannel()
    this.addBranch(channel)
    val branch = SpiderBranch(channel, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}