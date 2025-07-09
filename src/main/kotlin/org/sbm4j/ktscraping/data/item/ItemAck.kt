package org.sbm4j.ktscraping.data.item

import java.util.UUID

enum class ItemStatus{
    PROCESSED,
    ERROR,
    IGNORED
}

data class ItemAck(val itemId: UUID,
                   val status: ItemStatus,
                   val errors: List<ItemError> = emptyList())