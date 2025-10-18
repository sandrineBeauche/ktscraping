package org.sbm4j.ktscraping.data.internal

import org.sbm4j.ktscraping.data.Back
import org.sbm4j.ktscraping.data.Send
import org.sbm4j.ktscraping.data.Status
import java.util.*


abstract class Internal: Send {

    override var channelableId: UUID = UUID.randomUUID()

    override fun buildBack(): Back<*> {
        TODO("Not yet implemented")
    }

    override fun buildErrorBack(infos: ErrorInfo, status: Status): Back<*> {
        TODO("Not yet implemented")
    }

}




