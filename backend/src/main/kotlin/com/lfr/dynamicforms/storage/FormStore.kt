package com.lfr.dynamicforms.storage

import com.lfr.dynamicforms.model.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class FormStore(private val json: Json) {

    init {
        seedIfEmpty()
    }

    private fun seedIfEmpty() {
        val count = transaction(DatabaseFactory.database) { FormTable.selectAll().count() }
        if (count > 0) return

        val formsDir = this::class.java.classLoader?.getResource("forms") ?: return
        val dir = java.io.File(formsDir.toURI())
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val form = json.decodeFromString<FormDefinition>(file.readText())
                transaction(DatabaseFactory.database) {
                    FormTable.insert {
                        it[formId] = form.formId
                        it[title] = form.title
                        it[jsonData] = json.encodeToString(FormDefinition.serializer(), form)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to load form from ${file.name}: ${e.message}")
            }
        }
    }

    fun getAllSummaries(): List<FormSummary> = transaction(DatabaseFactory.database) {
        FormTable.selectAll().map { row ->
            val form = json.decodeFromString<FormDefinition>(row[FormTable.jsonData])
            FormSummary(
                formId = form.formId,
                title = form.title,
                description = form.description,
                pageCount = form.pages.size,
                fieldCount = form.pages.sumOf { page ->
                    page.elements.count { it !is SectionHeaderElement && it !is LabelElement }
                }
            )
        }
    }

    fun getForm(formId: String): FormDefinition? = transaction(DatabaseFactory.database) {
        FormTable.selectAll().where { FormTable.formId eq formId }
            .firstOrNull()
            ?.let { json.decodeFromString<FormDefinition>(it[FormTable.jsonData]) }
    }

    fun saveForm(form: FormDefinition) {
        transaction(DatabaseFactory.database) {
            val existing = FormTable.selectAll().where { FormTable.formId eq form.formId }.firstOrNull()
            val jsonData = json.encodeToString(FormDefinition.serializer(), form)
            if (existing != null) {
                FormTable.update({ FormTable.formId eq form.formId }) {
                    it[title] = form.title
                    it[FormTable.jsonData] = jsonData
                }
            } else {
                FormTable.insert {
                    it[FormTable.formId] = form.formId
                    it[title] = form.title
                    it[FormTable.jsonData] = jsonData
                }
            }
        }
    }

    fun deleteForm(formId: String): Boolean = transaction(DatabaseFactory.database) {
        SubmissionTable.deleteWhere { SubmissionTable.formId eq formId }
        FormTable.deleteWhere { FormTable.formId eq formId } > 0
    }

    fun getAllForms(): List<FormDefinition> = transaction(DatabaseFactory.database) {
        FormTable.selectAll().map { row ->
            json.decodeFromString<FormDefinition>(row[FormTable.jsonData])
        }
    }
}
