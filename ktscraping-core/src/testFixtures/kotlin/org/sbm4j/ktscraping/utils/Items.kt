package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item
import org.sbm4j.meercat.data.Send
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

data class DataItemTest(
    val value: String,
    val reqName: String,
    val url: String = "une url",
): Data(){
    override fun clone(): Data {
        val result = this.copy()
        return result
    }
}

data class IntDataItem(
    override var data: Int,
    override var sender: SendSource,
    override val name: String = "IntData-#$data"
): DataItem<Int>(){

    companion object {
        fun predicateOnValue(value: Int): (Send) -> Boolean {
            return { send: Send -> send is IntDataItem && send.data == value }
        }
    }

    override fun clone(): Item {
        return this.copy()
    }

    override fun getKeyBarrier(): String {
        return data.toString()
    }

}