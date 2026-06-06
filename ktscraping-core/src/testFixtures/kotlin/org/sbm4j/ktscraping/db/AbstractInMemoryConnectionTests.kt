package org.sbm4j.ktscraping.db

import org.junit.jupiter.api.BeforeEach
import org.sbm4j.ktscraping.domain.Contact
import org.sbm4j.ktscraping.domain.buildDBItems

abstract class AbstractInMemoryConnectionTests<T: DBConnexion>: AbstractDBConnectionTests<T>() {

    abstract fun prepareDB(contacts: List<Contact>)

    @BeforeEach
    fun setUpConnection() {
        conn = buildDBConnection()
        val contacts = buildDBItems(listOf(20, 35, 40))
        prepareDB(contacts)
    }
}