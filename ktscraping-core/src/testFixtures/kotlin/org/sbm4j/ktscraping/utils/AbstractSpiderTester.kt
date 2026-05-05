package org.sbm4j.ktscraping.utils

import org.sbm4j.ktscraping.core.components.AbstractSpider
import org.sbm4j.meercat.nodes.InitiatorTester

abstract class AbstractSpiderTester<T: AbstractSpider>:
    InitiatorTester<T>(),
    ComponentSourceTester<T> {
}