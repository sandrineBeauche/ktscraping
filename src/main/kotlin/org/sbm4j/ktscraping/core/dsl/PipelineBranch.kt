package org.sbm4j.ktscraping.core.dsl

import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.sbm4j.ktscraping.core.Crawler
import org.sbm4j.meercat.channels.SuperChannel
import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.meercat.components.Controllable
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcher
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherAll
import org.sbm4j.ktscraping.core.dispatchers.PipelineDispatcherOne
import org.sbm4j.ktscraping.data.item.Item


fun buildPipelineChannels(): SuperChannel{
    return SuperChannel()
}


fun Crawler.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val branch = PipelineBranch(
        this.channelManager.pipelineChannel,
        this.di)
    branch.initBranch()
    this.controllables.addAll(branch.senders)
}

fun Crawler.pipelineDispatcherAll(name: String = "dispatcher", initDispatcher: PipelineDispatcherAll.() -> Unit){
    val dispatcher = PipelineDispatcherAll(name, this.di)
    dispatcher.channelIn = this.channelManager.pipelineChannel

    dispatcher.initDispatcher()
    this.controllables.add(dispatcher)
}


fun Crawler.pipelineDispatcherOne(
    name: String = "dispatcher",
    selectChannelFunc: PipelineDispatcherOne.(Item) -> SuperChannel,
    init: PipelineDispatcherOne.() -> Unit
){
    val dispatcher = object : PipelineDispatcherOne(name, this.di){
        override fun selectChannel(item: Item): SuperChannel {
            return selectChannelFunc(item)
        }
    }

    dispatcher.channelIn = this.channelManager.pipelineChannel

    dispatcher.init()
    this.controllables.add(dispatcher)
}


class PipelineBranch(
    var pipelineChannel: SuperChannel,
    override val di: DI
): DIAware{

    val senders : MutableList<Controllable> = mutableListOf()

    inline fun <reified T: AbstractPipeline>pipeline(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T{
        val pip = buildControllable<T>(name)

        senders.add(pip)
        pip.inChannel = pipelineChannel

        val newChannel = SuperChannel()
        pip.outChannel = newChannel
        pipelineChannel = newChannel

        pip.init()

        return pip
    }

    inline fun <reified T: AbstractExporter>exporter(
                                      name: String? = null,
                                      init: T.() -> Unit = {}): T {
        val exp = buildControllable<T>(name)

        senders.add(exp)

        exp.inChannel = pipelineChannel
        exp.init()

        return exp
    }

    fun pipelineDispatcherAll(name: String = "dispatcher", init: PipelineDispatcherAll.() -> Unit){
        val dispatcher = PipelineDispatcherAll(name, this.di)
        dispatcher.channelIn = pipelineChannel
        dispatcher.init()
        senders.add(dispatcher)
    }


    fun pipelineDispatcherOne(
        name: String = "dispatcher",
        selectChannelFunc: PipelineDispatcherOne.(Item) -> SuperChannel,
        init: PipelineDispatcherOne.() -> Unit
    ){
        val dispatcher = object : PipelineDispatcherOne(name, this.di){
            override fun selectChannel(item: Item): SuperChannel {
                return selectChannelFunc(item)
            }
        }

        dispatcher.channelIn = pipelineChannel
        dispatcher.init()
        senders.add(dispatcher)
    }
}


inline fun <reified T: AbstractExporter> PipelineDispatcher.exporter(
                                                  name: String? = null,
                                                  init: T.() -> Unit = {}): T? {
    val exp = buildControllable<T>(name)

    val crawler: Crawler by di.instance(arg = this.di)
    crawler.controllables.add(exp)

    val newChannel = SuperChannel()

    exp.inChannel = newChannel

    this.addBranch(newChannel)
    exp.init()

    return exp
}


fun PipelineDispatcher.pipelineBranch(initBranch: PipelineBranch.() -> Unit){
    val newChannel = SuperChannel()
    this.addBranch(newChannel)
    val branch = PipelineBranch(newChannel, this.di)
    branch.initBranch()

    val crawler : Crawler by this.di.instance(arg = this.di)
    crawler.controllables.addAll(branch.senders)
}