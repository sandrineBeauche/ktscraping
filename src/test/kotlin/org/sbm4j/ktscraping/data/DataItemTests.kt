package org.sbm4j.ktscraping.data

import io.mockk.mockk
import org.sbm4j.ktscraping.data.item.Data
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.meercat.nodes.sendProcessors.SendSource
import kotlin.test.Test

data class DataTestType(val value: String): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}

class DataItemTests {

    val sender = mockk<SendSource>()

    @Test
    fun testDataItem1(){
        val data1 = ObjectDataItem.build(DataTestType("value1"), "test", sender)
        val cl = data1.data::class
        println(cl)
    }
}