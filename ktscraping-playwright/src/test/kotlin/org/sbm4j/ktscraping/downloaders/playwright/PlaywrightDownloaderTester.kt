package org.sbm4j.ktscraping.downloaders.playwright

import org.sbm4j.ktscraping.utils.AbstractDownloaderTester

abstract class AbstractPlaywrightRequestDownloadTester(val headless: Boolean): AbstractDownloaderTester<PlaywrightDownloader>(){

    override fun buildNode() : PlaywrightDownloader {
        return PlaywrightDownloader()
    }
}