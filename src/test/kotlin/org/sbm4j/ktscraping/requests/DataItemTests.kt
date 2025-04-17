package org.sbm4j.ktscraping.requests

import kotlin.test.Test

data class DataTestType(val value: String): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}

class DataItemTests {

    @Test
    fun testDataItem1(){
        val data1 = DataItem.build(DataTestType("value1"), "test")
        val cl = data1.data::class
        println(cl)
    }
}