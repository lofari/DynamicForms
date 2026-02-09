package com.lfr.dynamicforms.storage

import com.lfr.dynamicforms.model.Submission
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class SubmissionStore(private val json: Json) {

    fun addSubmission(formId: String, values: Map<String, String>): Submission {
        val submission = Submission(
            id = UUID.randomUUID().toString(),
            formId = formId,
            values = values,
            submittedAt = System.currentTimeMillis()
        )
        transaction(DatabaseFactory.database) {
            SubmissionTable.insert {
                it[SubmissionTable.id] = submission.id
                it[SubmissionTable.formId] = submission.formId
                it[valuesJson] = json.encodeToString(submission.values)
                it[submittedAt] = submission.submittedAt
            }
        }
        return submission
    }

    fun getSubmissions(formId: String): List<Submission> = transaction(DatabaseFactory.database) {
        SubmissionTable.selectAll().where { SubmissionTable.formId eq formId }
            .map { row ->
                Submission(
                    id = row[SubmissionTable.id],
                    formId = row[SubmissionTable.formId],
                    values = json.decodeFromString<Map<String, String>>(row[SubmissionTable.valuesJson]),
                    submittedAt = row[SubmissionTable.submittedAt]
                )
            }
    }

    fun getSubmissionCount(formId: String): Int = transaction(DatabaseFactory.database) {
        SubmissionTable.selectAll().where { SubmissionTable.formId eq formId }.count().toInt()
    }

    fun deleteSubmissions(formId: String) {
        transaction(DatabaseFactory.database) {
            SubmissionTable.deleteWhere { SubmissionTable.formId eq formId }
        }
    }
}
