package org.sbm4j.ktscraping.domain

import kotlinx.serialization.Serializable
import org.sbm4j.ktscraping.data.item.Data

@Serializable
data class Address(
    var street: String,
    var number: Int,
    var zipCode: Int,
    var city: String
)

@Serializable
data class Contact(
    var contactId: Int,
    var firstname: String,
    var lastname: String,
    var years: Int,
    var address: Address? = null
): Data() {
    override fun clone(): Data {
        return this.copy()
    }
}