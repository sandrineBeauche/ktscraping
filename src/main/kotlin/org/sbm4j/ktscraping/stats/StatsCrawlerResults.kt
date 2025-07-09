package org.sbm4j.ktscraping.stats

import org.sbm4j.ktscraping.core.CrawlerResult
import org.sbm4j.ktscraping.data.item.ItemError

data class StatsCrawlerResult(
    var nbRequests: Int = 0,
    var nbItems: Int = 0,
    val errors: MutableList<ItemError> = mutableListOf(),
    var responseOK: Int = 0,
    var responseError: Int = 0,
    var nbGoogleAPIRequests: Int = 0,
    val labelsNew: MutableMap<String, Int> = mutableMapOf(),
    val labelsUpdate: MutableMap<String, Int> = mutableMapOf(),
    val labelsDelete: MutableMap<String, Int> = mutableMapOf(),
): CrawlerResult {

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("Scraping statistics:\n")
        builder.append("$nbRequests requests, where $responseOK OK and $responseError erreurs, $nbGoogleAPIRequests Google API requests\n")
        builder.append("$nbItems items:\n")
        if(labelsNew.size > 0){
            val l = labelsNew.map{"${it.key}(s) ${it.value}"}.joinToString(", ")
            builder.append("created ${l}\n")
        }
        if(labelsUpdate.size > 0){
            val l = labelsUpdate.map{"${it.key}(s) ${it.value}"}.joinToString(", ")
            builder.append("updated ${l}\n")
        }
        if(labelsDelete.size > 0){
            val l = labelsDelete.map{"${it.key}(s) ${it.value}" +
                    ""}.joinToString(", ")
            builder.append("deleted ${l}\n")
        }

        if(errors.size > 0){
            builder.append("\nerrors:\n")
            errors.forEach {
                val infos = it.errorInfo
                builder.append("${infos.level}: error in ${infos.controllable.name} for the data ${it.data}\n")
                builder.append(infos.ex.printStackTrace())
                builder.append("\n")
            }
        }
        return builder.toString()
    }

    fun incrLabel(label: String, m: MutableMap<String, Int>){
        val value = (m.getOrPut(label){0})
        m[label] = value + 1
    }

    fun incrNew(label: String){
        incrLabel(label, labelsNew)
    }

    fun incrUpdate(label: String){
        incrLabel(label, labelsUpdate)
    }

    fun incrDelete(label: String){
        incrLabel(label, labelsDelete)
    }
}