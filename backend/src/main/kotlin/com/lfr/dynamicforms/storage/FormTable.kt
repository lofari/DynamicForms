package com.lfr.dynamicforms.storage

import org.jetbrains.exposed.v1.core.Table

object FormTable : Table("forms") {
    val formId = text("form_id")
    val title = text("title")
    val jsonData = text("json_data")

    override val primaryKey = PrimaryKey(formId)
}
