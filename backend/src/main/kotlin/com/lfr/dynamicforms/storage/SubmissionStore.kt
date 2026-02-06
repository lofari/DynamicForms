package com.lfr.dynamicforms.storage

import com.lfr.dynamicforms.model.Submission
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SubmissionStore {

    private val submissions = ConcurrentHashMap<String, MutableList<Submission>>()

    fun addSubmission(formId: String, values: Map<String, String>): Submission {
        val submission = Submission(
            id = UUID.randomUUID().toString(),
            formId = formId,
            values = values,
            submittedAt = System.currentTimeMillis()
        )
        submissions.getOrPut(formId) { mutableListOf() }.add(submission)
        return submission
    }

    fun getSubmissions(formId: String): List<Submission> =
        submissions[formId]?.toList() ?: emptyList()

    fun getSubmissionCount(formId: String): Int =
        submissions[formId]?.size ?: 0

    fun deleteSubmissions(formId: String) {
        submissions.remove(formId)
    }
}
