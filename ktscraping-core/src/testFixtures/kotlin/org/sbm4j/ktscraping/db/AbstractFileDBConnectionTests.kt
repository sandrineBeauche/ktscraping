package org.sbm4j.ktscraping.db

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.sbm4j.ktscraping.domain.buildDBItems
import org.sbm4j.ktscraping.domain.buildInsertContact
import java.io.File

abstract class AbstractFileDBConnectionTests<T: DBConnexion>: AbstractDBConnectionTests<T>() {

    open val mainDBFileName: String? = null

    open val tempDBFileName: String? = null

    var temporaryDBFile: File? = null

    @BeforeEach
    fun setUpConnection() {
        if(mainDBFileName != null) {
            val f = File(getRootResources(), mainDBFileName)
            temporaryDBFile = File(getRootResources(), tempDBFileName)
            if(!f.exists()){
                f.createNewFile()
            }
            if (f.exists() && temporaryDBFile != null) {
                f.copyTo(temporaryDBFile!!, overwrite = true)
            }
        }
        conn = buildDBConnection()
    }

    @AfterEach
    fun tearDownFiles() {
        conn.close()
        temporaryDBFile?.delete()
    }

    fun generateDB() = TestScope().runTest {
        val items = buildDBItems().map{buildInsertContact(it, sender)}
        items.forEach {
            conn.performInsertItem(it)
        }
        conn.commit()
    }
}