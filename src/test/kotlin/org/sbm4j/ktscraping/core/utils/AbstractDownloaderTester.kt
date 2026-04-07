package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.ktscraping.core.unit.components.TestingDownloader
import org.sbm4j.ktscraping.dowloaders.playwright.PlaywrightDownloader
import org.sbm4j.meercat.nodes.ConsumerNodeTester

abstract class AbstractDownloaderTester<T: AbstractDownloader>:
    ConsumerNodeTester<T>(),
    ConsumerComponentTester
{

}

abstract class AbstractPlaywrightRequestDownloadTester(val headless: Boolean): AbstractDownloaderTester<PlaywrightDownloader>(){

    override fun buildNode() : PlaywrightDownloader {
        return PlaywrightDownloader()
    }
}