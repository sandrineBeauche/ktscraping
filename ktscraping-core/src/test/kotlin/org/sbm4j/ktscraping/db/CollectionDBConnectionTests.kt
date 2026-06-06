package org.sbm4j.ktscraping.db

import org.sbm4j.ktscraping.domain.Contact


class CollectionDBConnectionTests : AbstractInMemoryConnectionTests<CollectionDBConnection>() {

    override fun buildDBConnection(): CollectionDBConnection {
        return CollectionDBConnection()
    }

    override fun prepareDB(contacts: List<Contact>) {
        conn.data.addAll(contacts)
    }

    override fun resetConnection() {
        conn.data.clear()
    }
}