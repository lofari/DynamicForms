package com.lfr.dynamicforms.storage

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteDataSource
import java.sql.Connection

object DatabaseFactory {

    lateinit var database: Database
        private set

    fun init(jdbcUrl: String = "jdbc:sqlite:dynamicforms.db") {
        val dataSource = SQLiteDataSource()
        dataSource.url = jdbcUrl
        dataSource.setEnforceForeignKeys(true)

        database = Database.connect(dataSource)

        transaction(database) {
            connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            SchemaUtils.create(FormTable, SubmissionTable)
        }
    }
}
