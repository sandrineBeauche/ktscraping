package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.AbstractPipeline
import org.sbm4j.meercat.nodes.MiddleNodeTester

abstract class AbstractPipelineTester<T: AbstractPipeline>:
    MiddleNodeTester<T>(),
    ConsumerComponentTester,
    ComponentSourceTester<T> {
}