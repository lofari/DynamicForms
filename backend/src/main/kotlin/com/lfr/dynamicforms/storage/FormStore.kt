package com.lfr.dynamicforms.storage

import com.lfr.dynamicforms.model.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class FormStore(private val json: Json) {

    private val forms = ConcurrentHashMap<String, FormDefinition>()

    init {
        loadForms()
    }

    private fun loadForms() {
        val formsDir = this::class.java.classLoader?.getResource("forms") ?: return
        val dir = java.io.File(formsDir.toURI())
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val form = json.decodeFromString<FormDefinition>(file.readText())
                forms[form.formId] = form
            } catch (e: Exception) {
                System.err.println("Failed to load form from ${file.name}: ${e.message}")
            }
        }
    }

    fun getAllSummaries(): List<FormSummary> = forms.values.map { form ->
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

    fun getForm(formId: String): FormDefinition? = forms[formId]

    fun saveForm(form: FormDefinition) {
        forms[form.formId] = form
    }

    fun deleteForm(formId: String): Boolean = forms.remove(formId) != null

    fun getAllForms(): List<FormDefinition> = forms.values.toList()
}
