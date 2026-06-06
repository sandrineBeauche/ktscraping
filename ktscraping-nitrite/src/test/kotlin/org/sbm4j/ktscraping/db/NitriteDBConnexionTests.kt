package org.sbm4j.ktscraping.db


class NitriteDBConnexionTests: AbstractFileDBConnectionTests<NitriteDBConnexion>() {

    override val mainDBFileName: String = "contacts.nitrite"

    override val tempDBFileName = "tempContacts.nitrite"


    override fun buildDBConnection(): NitriteDBConnexion{
        return NitriteDBConnexion(this.temporaryDBFile!!)
    }

    override fun resetConnection(){
        NitriteDBConnexion.reset()
    }

}