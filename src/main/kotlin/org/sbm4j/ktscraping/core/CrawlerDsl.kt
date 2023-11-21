package org.sbm4j.ktscraping.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.Request
import org.sbm4j.ktscraping.requests.Response
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

fun buildSpiderChannels(): Triple<Channel<Response>, Channel<Request>, Channel<Item>>{
    return Triple(Channel<Response>(Channel.UNLIMITED),
            Channel<Request>(Channel.UNLIMITED),
            Channel<Item>(Channel.UNLIMITED)
            )
}


fun crawler(scope: CoroutineScope,
            name: String = "Crawler",
            diModuleFactory: (CoroutineScope, String) -> DI.Module,
            init: Crawler.() -> Unit): Crawler{
    val di = DI{
        import(diModuleFactory(scope, name))
    }
    val result : Crawler by di.instance(arg = di)
    result.init()
    return result
}


fun DefaultCrawler.configuration(initConf: CrawlerConfiguration.() -> Unit){
    this.configuration.initConf()
}

fun Crawler.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val branch = SpiderBranch(this.scope,
        this.channelFactory.spiderRequestChannel,
        this.channelFactory.spiderResponseChannel,
        this.channelFactory.spiderItemChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.spiderDispatcher(name: String = "dispatcher", initDispatcher: SpiderResponseDispatcher.() -> Unit){
    val dispatcher = SpiderResponseDispatcher(this.scope, name, this.di)
    dispatcher.channelOut = this.channelFactory.spiderRequestChannel
    dispatcher.channelIn = this.channelFactory.spiderResponseChannel
    dispatcher.itemChannelOut = this.channelFactory.spiderItemChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


class SpiderBranch(val scope: CoroutineScope,
                   var spiderIn: Channel<Request>,
                   var spiderOut: Channel<Response>,
                   var spiderItemIn: Channel<Item>,
                   override val di: DI
) : DIAware{

    val senders : MutableList<Controllable> = mutableListOf()

    fun <T : SpiderMiddleware>spiderMiddleware(clazz: KClass<T>,
                                               name: String = "SpiderMiddleware",
                                               init: T.() -> Unit): T?{
        val mid = clazz.primaryConstructor?.call(scope, name)
        if(mid != null) {
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
        }
        return mid
    }

    fun <T: AbstractSpider>spider(clazz: KClass<T>,
                                  name:String = "Spider",
                                  init: T.() -> Unit): T? {
        val spid = clazz.primaryConstructor?.call(scope, name)
        if(spid !=null){
            senders.add(spid)
            spid.requestOut = spiderIn
            spid.responseIn = spiderOut
            spid.itemsOut = spiderItemIn

            spid.init()
        }
        return spid
    }

    fun spiderDispatcher(name: String = "dispatcher", init: SpiderResponseDispatcher.() -> Unit){
        val dispatcher = SpiderResponseDispatcher(scope, name, this.di)
        dispatcher.channelOut = spiderIn
        dispatcher.channelIn = spiderOut
        dispatcher.itemChannelOut = spiderItemIn
        dispatcher.init()
        senders.add(dispatcher)
    }

}

fun <T: AbstractSpider>SpiderResponseDispatcher.spider(clazz: KClass<T>,
                                                       name: String = "Spider",
                                                       init: T.() -> Unit): T? {
    val spid = clazz.primaryConstructor?.call(scope, name)
    if(spid !=null){
        val crawler : Crawler by di.instance(arg = this.di)
        crawler.controllables.add(spid)

        val (spidResp, spidReq, spidItem) = buildSpiderChannels()
        spid.requestOut = spidReq
        spid.responseIn = spidResp
        val itemChannel = spidItem
        spid.itemsOut = itemChannel

        this.addBranch(spidReq, spidResp, itemChannel)
        spid.init()
    }
    return spid
}

fun SpiderResponseDispatcher.spiderBranch(initBranch: SpiderBranch.() -> Unit){
    val (spidResp, spidReq, spidItem) = buildSpiderChannels()
    this.addBranch(spidReq, spidResp, spidItem)
    val branch = SpiderBranch(this.scope, spidReq, spidResp, spidItem, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}