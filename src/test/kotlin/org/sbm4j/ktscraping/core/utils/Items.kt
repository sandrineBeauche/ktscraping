package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.Controllable
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.DataItem
import org.sbm4j.ktscraping.data.item.Item

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
    override val data: Int,
    override var sender: Controllable,
    override val name: String = "IntData-#$data"
): DataItem<Int>(){
    override fun clone(): Item {
        return this.copy()
    }

}