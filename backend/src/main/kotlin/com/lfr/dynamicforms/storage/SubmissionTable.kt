package com.lfr.dynamicforms.storage

import org.jetbrains.exposed.v1.core.Table

object SubmissionTable : Table("submissions") {
    val id = text("id")
    val formId = text("form_id").references(FormTable.formId)
    val valuesJson = text("values_json")
    val submittedAt = long("submitted_at")

    override val primaryKey = PrimaryKey(id)
}
