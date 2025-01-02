package org.sbm4j.ktscraping.core

class ProgressMonitor() {
    var receivedRequest: Int = 0
    var receivedResponse: Int = 0
    var receivedItem: Int = 0
    var receivedItemAck: Int = 0

    var noMoreRequest: Boolean = false
    var noMoreItem: Boolean = false
    var scrapingDone: Boolean = false

}