package org.sbm4j.ktscraping.utils

import org.sbm4j.ktscraping.core.components.AbstractDownloader
import org.sbm4j.meercat.nodes.ConsumerNodeTester

abstract class AbstractDownloaderTester<T: AbstractDownloader>:
    ConsumerNodeTester<T>(),
    ConsumerComponentTester
{

}

