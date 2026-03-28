package org.sbm4j.ktscraping.data.internal

import org.sbm4j.meercat.data.Channelable
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.meercat.nodes.sendProcessors.SendSource


data class ErrorInternal(
    val errorInfo: ErrorInfo,
    override var sender: SendSource,
    val data: Channelable? = null
): Internal(){

    override val name: String = "ErrorInternalData-${Channelable.lastId.getAndIncrement()}"

    override fun clone(): ErrorInternal {
        return this.copy()
    }

    override fun getKeyBarrier(): String {
        return this.errorInfo.ex.javaClass.simpleName
    }
}
