package org.sbm4j.ktscraping.core.utils

import org.sbm4j.ktscraping.core.components.AbstractExporter
import org.sbm4j.ktscraping.utils.ConsumerComponentTester
import org.sbm4j.meercat.nodes.ConsumerNodeTester

abstract class AbstractExporterTester<T: AbstractExporter>:
    ConsumerNodeTester<T>(),
    ConsumerComponentTester
{
}