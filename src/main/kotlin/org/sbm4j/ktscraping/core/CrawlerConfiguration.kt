package org.sbm4j.ktscraping.core

data class CrawlerConfiguration(
    var nbConnexions: Int = 10) {
    var autoThrottle: Int = 2000
}