package org.sbm4j.ktscraping.data.item

import org.sbm4j.ktscraping.data.Channelable
import java.util.*

abstract class Item : Channelable {

    var itemId: UUID = UUID.randomUUID()

    abstract fun clone(): Item
}
