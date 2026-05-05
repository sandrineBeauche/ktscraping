package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.AbstractMiddleware
import org.sbm4j.ktscraping.utils.ComponentSourceTester
import org.sbm4j.ktscraping.utils.ConsumerComponentTester
import org.sbm4j.meercat.nodes.MiddleNodeTester

abstract class AbstractDownloaderMiddlewareTester<T: AbstractMiddleware>:
    MiddleNodeTester<T>(),
    ConsumerComponentTester,
    ComponentSourceTester<T> {

}