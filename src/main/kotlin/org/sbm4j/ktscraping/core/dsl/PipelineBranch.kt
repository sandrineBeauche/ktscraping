package org.sbm4j.ktscraping.core.dsl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.*
import org.sbm4j.ktscraping.data.item.AbstractItemAck
import org.sbm4j.ktscraping.data.item.ErrorInfo
import org.sbm4j.ktscraping.data.item.Item


fun buildPipelineChannels(): Pair<Channel<Item>, Channel<AbstractItemAck<*>>>{
    return Pair(
        Channel<Item>(Channel.UNLIMITED),
        Channel<AbstractItemAck<*>>(Channel.UNLIMITED)
    )
}


fun Crawler.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val branch = PipelineBranch(
        this.channelFactory.itemChannel,
        this.channelFactory.itemAckChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.pipelineDispatcherAll(name: String = "dispatcher", initDispatcher: ItemDispatcherAll.() -> Unit){
    val dispatcher = ItemDispatcherAll(name, this.di)
    //dispatcher.itemIn = this.channelFactory.itemChannel
    dispatcher.itemAckOut = this.channelFactory.itemAckChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


fun Crawler.pipelineDispatcherOne(
    name: String = "dispatcher",
    selectChannelFunc: ItemDispatcherOne.(Item) -> SendChannel<Item>,
    init: ItemDispatcherOne.() -> Unit
){
    val dispatcher = object : ItemDispatcherOne(name, this.di){
        override fun selectChannel(item: Item): SendChannel<Item> {
            return selectChannelFunc(item)
        }

        override var inChannel: SuperChannel
            get() = TODO("Not yet implemented")
            set(value) {}

        override fun generateErrorInfos(ex: Exception): ErrorInfo {
            TODO("Not yet implemented")
        }
    }
    //dispatcher.itemIn = this.channelFactory.itemChannel
    dispatcher.itemAckOut = this.channelFactory.itemAckChannel

    dispatcher.init()
    this.controllables.add(dispatcher)
}


class PipelineBranch(
    var pipelineItemIn: Channel<Item>,
    var pipelineItemAckOut: Channel<AbstractItemAck<*>>,
    override val di: DI
): DIAware{

    val senders : MutableList<Controllable> = mutableListOf()

    inline fun <reified T: AbstractPipeline>pipeline(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T{
        val pip = buildControllable<T>(name)

        /*
        senders.add(pip)
        pip.itemIn = pipelineItemIn
        pip.itemAckOut = pipelineItemAckOut

        val (newPipItem, newPipItemAck) = buildPipelineChannels()
        pip.itemOut = newPipItem
        pip.itemAckIn = newPipItemAck

        pipelineItemIn = newPipItem
        pipelineItemAckOut = newPipItemAck


         */
        pip.init()

        return pip
    }

    inline fun <reified T: AbstractExporter>exporter(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T {
        val exp = buildControllable<T>(name)

        senders.add(exp)
        /*
        exp.itemIn = pipelineItemIn
        exp.itemAckOut = pipelineItemAckOut
        exp.init()


         */
        return exp
    }

    fun pipelineDispatcherAll(name: String = "dispatcher", init: ItemDispatcherAll.() -> Unit){
        val dispatcher = ItemDispatcherAll(name, this.di)
        //dispatcher.itemIn = pipelineItemIn
        dispatcher.itemAckOut = pipelineItemAckOut
        dispatcher.init()
        senders.add(dispatcher)
    }


    fun pipelineDispatcherOne(
        name: String = "dispatcher",
        selectChannelFunc: ItemDispatcherOne.(Item) -> SendChannel<Item>,
        init: ItemDispatcherOne.() -> Unit
    ){
        val dispatcher = object : ItemDispatcherOne(name, this.di){
            override fun selectChannel(item: Item): SendChannel<Item> {
                return selectChannelFunc(item)
            }

            override var inChannel: SuperChannel
                get() = TODO("Not yet implemented")
                set(value) {}

            override fun generateErrorInfos(ex: Exception): ErrorInfo {
                TODO("Not yet implemented")
            }
        }
        //dispatcher.itemIn = pipelineItemIn
        dispatcher.itemAckOut = pipelineItemAckOut
        dispatcher.init()
        senders.add(dispatcher)
    }
}


inline fun <reified T: AbstractExporter> ItemDispatcher.exporter(
                                                  name: String? = null,
                                                  init: T.() -> Unit = {}): T? {
    val exp = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(exp)

    val (newItemChannel, newItemAckChannel) = buildPipelineChannels()
    /*
    exp.itemIn = newItemChannel
    exp.itemAckOut = newItemAckChannel


     */
    this.addBranch(newItemChannel, newItemAckChannel)
    exp.init()

    return exp
}


fun ItemDispatcher.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val (newItemChannel, newItemAckChannel) = buildPipelineChannels()
    this.addBranch(newItemChannel, newItemAckChannel)
    val branch = PipelineBranch(newItemChannel, newItemAckChannel, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}