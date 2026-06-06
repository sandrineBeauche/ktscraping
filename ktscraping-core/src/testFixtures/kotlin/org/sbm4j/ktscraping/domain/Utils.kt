package org.sbm4j.ktscraping.domain

import io.github.serpro69.kfaker.Faker
import org.sbm4j.ktscraping.data.item.ObjectDataItem
import org.sbm4j.meercat.nodes.sendProcessors.SendSource

val faker = Faker()

fun buildContact(id: Int, age: Int): Contact {
    val firstname = faker.name.firstName()
    val lastname = faker.name.lastName()
    return Contact(id, firstname, lastname, age)
}

fun buildInsertContact(contact: Contact, sender: SendSource): ObjectDataItem<Contact> {
    return ObjectDataItem(contact, Contact::class,
        "contact", sender)
}

fun buildDBItems(ages: List<Int> = listOf(20, 35, 40)): List<Contact>{
    val items = ages.mapIndexed { index, i ->
        buildContact(index, i)
    }

    val address = Address(
        faker.address.streetName(),
        faker.address.buildingNumber().toInt(),
        10000,
        faker.address.city(),
    )
    items[2].address = address

    return items
}