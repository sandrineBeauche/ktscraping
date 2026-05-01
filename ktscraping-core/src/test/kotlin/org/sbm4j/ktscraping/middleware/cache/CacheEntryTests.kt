package org.sbm4j.ktscraping.middleware.cache

import org.junit.jupiter.api.Assertions.assertTrue
import org.sbm4j.ktscraping.middleware.CacheAvailability
import org.sbm4j.ktscraping.middleware.CacheEntry
import kotlin.test.Test
import kotlin.test.assertFalse

class CacheEntryTests {

    @Test
    fun testOutOfDate1(){
        val entry = CacheEntry(availability = CacheAvailability.HOUR)
        val result = entry.isOutOfDate()

        assertFalse { result }
    }

    @Test
    fun testOutOfDate2(){
        val entry = CacheEntry(availability = CacheAvailability.NEVER)
        val result = entry.isOutOfDate()

        assertTrue { result }
    }
}