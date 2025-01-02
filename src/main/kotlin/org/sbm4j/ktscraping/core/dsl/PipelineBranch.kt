package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.requests.Item
import org.sbm4j.ktscraping.requests.ItemAck
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


fun buildPipelineChannels(): Pair<Channel<Item>, Channel<ItemAck>>{
    return Pair(
        Channel<Item>(Channel.UNLIMITED),
        Channel<ItemAck>(Channel.UNLIMITED)
    )
}


fun Crawler.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val branch = PipelineBranch(this.scope,
        this.channelFactory.itemChannel,
        this.channelFactory.itemAckChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.pipelineDispatcherAll(name: String = "dispatcher", initDispatcher: ItemDispatcherAll.() -> Unit){
    val dispatcher = ItemDispatcherAll(this.scope, name, this.di)
    dispatcher.itemIn = this.channelFactory.itemChannel
    dispatcher.itemAckOut = this.channelFactory.itemAckChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


fun Crawler.pipelineDispatcherOne(
    name: String = "dispatcher",
    selectChannelFunc: ItemDispatcherOne.(Item) -> SendChannel<Item>,
    init: ItemDispatcherOne.() -> Unit
){
    val dispatcher = object : ItemDispatcherOne(this.scope, name, this.di){
        override fun selectChannel(item: Item): SendChannel<Item> {
            return selectChannelFunc(item)
        }
    }
    dispatcher.itemIn = this.channelFactory.itemChannel
    dispatcher.itemAckOut = this.channelFactory.itemAckChannel

    dispatcher.init()
    this.controllables.add(dispatcher)
}


class PipelineBranch(
    val scope: CoroutineScope,
    var pipelineItemIn: Channel<Item>,
    var pipelineItemAckOut: Channel<ItemAck>,
    override val di: DI
): DIAware{

    val senders : MutableList<Controllable> = mutableListOf()

    inline fun <reified T: AbstractPipeline>pipeline(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T{
        val pip = buildControllable<T>(name, scope)

        senders.add(pip)
        pip.itemIn = pipelineItemIn
        pip.itemAckOut = pipelineItemAckOut

        val (newPipItem, newPipItemAck) = buildPipelineChannels()
        pip.itemOut = newPipItem
        pip.itemAckIn = newPipItemAck

        pipelineItemIn = newPipItem
        pipelineItemAckOut = newPipItemAck

        pip.init()

        return pip
    }

    inline fun <reified T: AbstractExporter>exporter(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T {
        val exp = buildControllable<T>(name, scope)

        senders.add(exp)
        exp.itemIn = pipelineItemIn
        exp.itemAckOut = pipelineItemAckOut
        exp.init()

        return exp
    }

    fun pipelineDispatcherAll(name: String = "dispatcher", init: ItemDispatcherAll.() -> Unit){
        val dispatcher = ItemDispatcherAll(scope, name, this.di)
        dispatcher.itemIn = pipelineItemIn
        dispatcher.itemAckOut = pipelineItemAckOut
        dispatcher.init()
        senders.add(dispatcher)
    }


    fun pipelineDispatcherOne(
        name: String = "dispatcher",
        selectChannelFunc: ItemDispatcherOne.(Item) -> SendChannel<Item>,
        init: ItemDispatcherOne.() -> Unit
    ){
        val dispatcher = object : ItemDispatcherOne(this.scope, name, this.di){
            override fun selectChannel(item: Item): SendChannel<Item> {
                return selectChannelFunc(item)
            }
        }
        dispatcher.itemIn = pipelineItemIn
        dispatcher.itemAckOut = pipelineItemAckOut
        dispatcher.init()
        senders.add(dispatcher)
    }
}


inline fun <reified T: AbstractExporter> ItemDispatcher.exporter(
                                                  name: String? = null,
                                                  init: T.() -> Unit = {}): T? {
    val exp = buildControllable<T>(name, scope)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(exp)

    val (newItemChannel, newItemAckChannel) = buildPipelineChannels()
    exp.itemIn = newItemChannel
    exp.itemAckOut = newItemAckChannel

    this.addBranch(newItemChannel, newItemAckChannel)
    exp.init()

    return exp
}


fun ItemDispatcher.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val (newItemChannel, newItemAckChannel) = buildPipelineChannels()
    this.addBranch(newItemChannel, newItemAckChannel)
    val branch = PipelineBranch(this.scope, newItemChannel, newItemAckChannel, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}