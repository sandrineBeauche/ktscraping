package org.sbm4j.meercat.data

import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

 data class SimpleSend<T>(var value: T, override var sender: SendSource) : Send{
    companion object{
        var lastId: AtomicInteger = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    override val name: String = "SimpleSend#${lastId.getAndIncrement()}"

    override fun buildErrorBack(
        infos: ErrorInfo,
        status: Status
    ): Back<*> {
        val result = SimpleBack(this)
        result.status = status
        result.errorInfos.add(infos)
        return result
    }

    override fun buildBack(): Back<*> {
        return SimpleBack(this)
    }

    override fun clone(): Send {
        return this.copy()
    }
}


data class SimpleBack<T>(override val send: SimpleSend<T>): Back<SimpleSend<T>> {
    companion object{
        var lastId: AtomicInteger = AtomicInteger(0)
    }

    override var channelableId: UUID = UUID.randomUUID()

    override val name: String = "SimpleBack#${lastId.getAndIncrement()}"

    override var status: Status = Status.OK

    override val errorInfos: MutableList<ErrorInfo> = mutableListOf()

    override fun clone(): Back<SimpleSend<T>> {
        return this.copy()
    }
}