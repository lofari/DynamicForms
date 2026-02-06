# DynamicForms Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an Android app that parses polymorphic JSON into dynamic multi-step form wizards with validation, conditional visibility, draft saving, and submission.

**Architecture:** MVI + Clean Architecture with Hilt DI. Domain layer (models, repository interfaces, use cases) has no framework dependencies except kotlinx.serialization. Data layer implements repositories with Retrofit (mock interceptor) and Room. Presentation layer uses Jetpack Compose + Material3 with type-safe Navigation.

**Tech Stack:** Kotlin 2.0.21, AGP 9.0, Compose BOM 2024.09.00, Hilt, Room, Retrofit, Kotlin Serialization, Navigation Compose (type-safe routes), KSP.

**Design doc:** `docs/plans/2026-02-05-dynamic-forms-design.md`

**Base source path:** `app/src/main/java/com/lfr/dynamicforms/`
**Base test path:** `app/src/test/java/com/lfr/dynamicforms/`

---

## Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`

**Step 1: Update version catalog**

Add to `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.0.0"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.10.0"
activityCompose = "1.12.3"
kotlin = "2.0.21"
composeBom = "2024.09.00"
hilt = "2.51.1"
room = "2.6.1"
navigationCompose = "2.8.4"
kotlinxSerializationJson = "1.7.3"
retrofit = "2.11.0"
okhttp = "4.12.0"
ksp = "2.0.21-1.0.28"
hiltNavigationCompose = "1.2.0"
retrofitSerializationConverter = "1.0.0"
coroutinesTest = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofitSerializationConverter" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**Step 2: Update root build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

**Step 3: Update app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.lfr.dynamicforms"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.lfr.dynamicforms"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

**Step 4: Sync and verify build**

Run: `./gradlew assembleDebug` from project root.
Expected: BUILD SUCCESSFUL. If any version conflicts occur, adjust versions in `libs.versions.toml`.

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "chore: add Hilt, Room, Navigation, Retrofit, KotlinX Serialization dependencies"
```

---

## Task 2: Domain Models

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/model/FormElement.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/model/Validation.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/model/Form.kt`

**Step 1: Create Validation.kt**

```kotlin
package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TextValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class NumberValidation(
    val min: Double? = null,
    val max: Double? = null,
    val errorMessage: String? = null
)

@Serializable
data class SelectionValidation(
    val minSelections: Int? = null,
    val maxSelections: Int? = null,
    val errorMessage: String? = null
)

@Serializable
data class SelectOption(
    val value: String,
    val label: String
)
```

**Step 2: Create FormElement.kt**

```kotlin
package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class VisibilityCondition(
    val fieldId: String,
    val operator: ConditionOperator,
    val value: JsonPrimitive
)

@Serializable
enum class ConditionOperator {
    @SerialName("equals") EQUALS,
    @SerialName("not_equals") NOT_EQUALS,
    @SerialName("greater_than") GREATER_THAN,
    @SerialName("less_than") LESS_THAN,
    @SerialName("contains") CONTAINS,
    @SerialName("is_empty") IS_EMPTY,
    @SerialName("is_not_empty") IS_NOT_EMPTY
}

@Serializable
sealed class FormElement {
    abstract val id: String
    abstract val label: String
    abstract val required: Boolean
    abstract val visibleWhen: VisibilityCondition?
}

@Serializable @SerialName("text_field")
data class TextFieldElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val multiline: Boolean = false,
    val validation: TextValidation? = null
) : FormElement()

@Serializable @SerialName("number_field")
data class NumberFieldElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val validation: NumberValidation? = null
) : FormElement()

@Serializable @SerialName("dropdown")
data class DropdownElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList()
) : FormElement()

@Serializable @SerialName("checkbox")
data class CheckboxElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val defaultValue: Boolean = false
) : FormElement()

@Serializable @SerialName("radio")
data class RadioElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList()
) : FormElement()

@Serializable @SerialName("date_picker")
data class DatePickerElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val minDate: String? = null,
    val maxDate: String? = null,
    val format: String = "yyyy-MM-dd"
) : FormElement()

@Serializable @SerialName("toggle")
data class ToggleElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val defaultValue: Boolean = false
) : FormElement()

@Serializable @SerialName("slider")
data class SliderElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f
) : FormElement()

@Serializable @SerialName("multi_select")
data class MultiSelectElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val options: List<SelectOption> = emptyList(),
    val validation: SelectionValidation? = null
) : FormElement()

@Serializable @SerialName("file_upload")
data class FileUploadElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val maxFileSize: Long = 10_485_760,
    val allowedTypes: List<String> = emptyList()
) : FormElement()

@Serializable @SerialName("signature")
data class SignatureElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null
) : FormElement()

@Serializable @SerialName("section_header")
data class SectionHeaderElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val subtitle: String? = null
) : FormElement()

@Serializable @SerialName("label")
data class LabelElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val text: String = ""
) : FormElement()

@Serializable @SerialName("repeating_group")
data class RepeatingGroupElement(
    override val id: String,
    override val label: String,
    override val required: Boolean = false,
    override val visibleWhen: VisibilityCondition? = null,
    val minItems: Int = 1,
    val maxItems: Int = 10,
    val elements: List<FormElement> = emptyList()
) : FormElement()

fun FormElement.getDefaultValue(): String? = when (this) {
    is ToggleElement -> defaultValue.toString()
    is CheckboxElement -> defaultValue.toString()
    is SliderElement -> min.toString()
    else -> null
}
```

**Step 3: Create Form.kt**

```kotlin
package com.lfr.dynamicforms.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Form(
    val formId: String,
    val title: String,
    val pages: List<Page>
)

@Serializable
data class Page(
    val pageId: String,
    val title: String,
    val elements: List<FormElement>
)

@Serializable
data class FormSummary(
    val formId: String,
    val title: String,
    val description: String = ""
)

@Serializable
data class FormSubmission(
    val formId: String,
    val values: Map<String, String>
)

@Serializable
data class SubmissionResponse(
    val success: Boolean,
    val message: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

data class Draft(
    val formId: String,
    val pageIndex: Int,
    val values: Map<String, String>,
    val updatedAt: Long
)
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/domain/
git commit -m "feat: add domain models - FormElement hierarchy, validation, form structure"
```

---

## Task 3: Domain Repository Interfaces

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/repository/FormRepository.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/repository/DraftRepository.kt`

**Step 1: Create FormRepository.kt**

```kotlin
package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse

interface FormRepository {
    suspend fun getForms(): List<FormSummary>
    suspend fun getForm(formId: String): Form
    suspend fun submitForm(formId: String, values: Map<String, String>): SubmissionResponse
}
```

**Step 2: Create DraftRepository.kt**

```kotlin
package com.lfr.dynamicforms.domain.repository

import com.lfr.dynamicforms.domain.model.Draft

interface DraftRepository {
    suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>)
    suspend fun getDraft(formId: String): Draft?
    suspend fun deleteDraft(formId: String)
    suspend fun getFormIdsWithDrafts(): List<String>
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/domain/repository/
git commit -m "feat: add domain repository interfaces"
```

---

## Task 4: Domain Use Cases

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/usecase/EvaluateVisibilityUseCase.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/usecase/ValidatePageUseCase.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/usecase/GetFormUseCase.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/usecase/SaveDraftUseCase.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/domain/usecase/SubmitFormUseCase.kt`

**Step 1: Create EvaluateVisibilityUseCase.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.ConditionOperator
import com.lfr.dynamicforms.domain.model.FormElement
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.VisibilityCondition
import javax.inject.Inject

class EvaluateVisibilityUseCase @Inject constructor() {

    fun getVisibleElements(page: Page, values: Map<String, String>): List<FormElement> {
        return page.elements.filter { isVisible(it, values) }
    }

    fun isVisible(element: FormElement, values: Map<String, String>): Boolean {
        val condition = element.visibleWhen ?: return true
        return evaluate(condition, values)
    }

    private fun evaluate(condition: VisibilityCondition, values: Map<String, String>): Boolean {
        val fieldValue = values[condition.fieldId] ?: ""
        val condValue = condition.value.content

        return when (condition.operator) {
            ConditionOperator.EQUALS -> fieldValue == condValue
            ConditionOperator.NOT_EQUALS -> fieldValue != condValue
            ConditionOperator.GREATER_THAN -> {
                val fv = fieldValue.toDoubleOrNull() ?: return false
                val cv = condValue.toDoubleOrNull() ?: return false
                fv > cv
            }
            ConditionOperator.LESS_THAN -> {
                val fv = fieldValue.toDoubleOrNull() ?: return false
                val cv = condValue.toDoubleOrNull() ?: return false
                fv < cv
            }
            ConditionOperator.CONTAINS -> fieldValue.contains(condValue, ignoreCase = true)
            ConditionOperator.IS_EMPTY -> fieldValue.isEmpty()
            ConditionOperator.IS_NOT_EMPTY -> fieldValue.isNotEmpty()
        }
    }
}
```

**Step 2: Create ValidatePageUseCase.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import javax.inject.Inject

class ValidatePageUseCase @Inject constructor(
    private val evaluateVisibility: EvaluateVisibilityUseCase
) {

    fun validate(page: Page, values: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val visibleElements = evaluateVisibility.getVisibleElements(page, values)

        for (element in visibleElements) {
            val value = values[element.id] ?: ""
            val error = validateElement(element, value)
            if (error != null) {
                errors[element.id] = error
            }
        }
        return errors
    }

    fun validateAllPages(pages: List<Page>, values: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for (page in pages) {
            errors.putAll(validate(page, values))
        }
        return errors
    }

    fun firstPageWithErrors(pages: List<Page>, errors: Map<String, String>): Int {
        for ((index, page) in pages.withIndex()) {
            val pageFieldIds = page.elements.map { it.id }.toSet()
            if (errors.keys.any { it in pageFieldIds }) return index
        }
        return 0
    }

    private fun validateElement(element: FormElement, value: String): String? {
        if (element.required && value.isBlank()) {
            return "${element.label} is required"
        }
        if (value.isBlank()) return null

        return when (element) {
            is TextFieldElement -> validateText(value, element.validation)
            is NumberFieldElement -> validateNumber(value, element.validation)
            is MultiSelectElement -> validateSelection(value, element.validation)
            else -> null
        }
    }

    private fun validateText(value: String, validation: TextValidation?): String? {
        validation ?: return null
        if (validation.minLength != null && value.length < validation.minLength) {
            return validation.errorMessage ?: "Minimum ${validation.minLength} characters"
        }
        if (validation.maxLength != null && value.length > validation.maxLength) {
            return validation.errorMessage ?: "Maximum ${validation.maxLength} characters"
        }
        if (validation.pattern != null && !Regex(validation.pattern).matches(value)) {
            return validation.errorMessage ?: "Invalid format"
        }
        return null
    }

    private fun validateNumber(value: String, validation: NumberValidation?): String? {
        val number = value.toDoubleOrNull() ?: return "Must be a number"
        validation ?: return null
        if (validation.min != null && number < validation.min) {
            return validation.errorMessage ?: "Minimum value is ${validation.min}"
        }
        if (validation.max != null && number > validation.max) {
            return validation.errorMessage ?: "Maximum value is ${validation.max}"
        }
        return null
    }

    private fun validateSelection(value: String, validation: SelectionValidation?): String? {
        validation ?: return null
        val selections = if (value.isBlank()) 0 else value.split(",").size
        if (validation.minSelections != null && selections < validation.minSelections) {
            return validation.errorMessage ?: "Select at least ${validation.minSelections}"
        }
        if (validation.maxSelections != null && selections > validation.maxSelections) {
            return validation.errorMessage ?: "Select at most ${validation.maxSelections}"
        }
        return null
    }
}
```

**Step 3: Create GetFormUseCase.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.getDefaultValue
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

data class FormWithDraft(
    val form: Form,
    val initialValues: Map<String, String>,
    val initialPageIndex: Int
)

class GetFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(formId: String): FormWithDraft {
        val form = formRepository.getForm(formId)

        val defaults = mutableMapOf<String, String>()
        for (page in form.pages) {
            for (element in page.elements) {
                element.getDefaultValue()?.let { defaults[element.id] = it }
            }
        }

        val draft = draftRepository.getDraft(formId)
        return if (draft != null) {
            FormWithDraft(
                form = form,
                initialValues = defaults + draft.values,
                initialPageIndex = draft.pageIndex
            )
        } else {
            FormWithDraft(
                form = form,
                initialValues = defaults,
                initialPageIndex = 0
            )
        }
    }
}
```

**Step 4: Create SaveDraftUseCase.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.repository.DraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(formId: String, pageIndex: Int, values: Map<String, String>) {
        draftRepository.saveDraft(formId, pageIndex, values)
    }
}
```

**Step 5: Create SubmitFormUseCase.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

class SubmitFormUseCase @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val validatePage: ValidatePageUseCase
) {
    suspend operator fun invoke(
        form: Form,
        values: Map<String, String>
    ): SubmitResult {
        val errors = validatePage.validateAllPages(form.pages, values)
        if (errors.isNotEmpty()) {
            val firstPage = validatePage.firstPageWithErrors(form.pages, errors)
            return SubmitResult.ValidationFailed(errors, firstPage)
        }

        val response = formRepository.submitForm(form.formId, values)
        return if (response.success) {
            draftRepository.deleteDraft(form.formId)
            SubmitResult.Success(response.message ?: "Form submitted successfully")
        } else {
            val firstPage = validatePage.firstPageWithErrors(form.pages, response.fieldErrors)
            SubmitResult.ServerError(response.fieldErrors, firstPage)
        }
    }
}

sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class ValidationFailed(val errors: Map<String, String>, val firstErrorPage: Int) : SubmitResult()
    data class ServerError(val fieldErrors: Map<String, String>, val firstErrorPage: Int) : SubmitResult()
}
```

**Step 6: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/domain/usecase/
git commit -m "feat: add domain use cases - visibility, validation, form loading, drafts, submission"
```

---

## Task 5: Domain Unit Tests

**Files:**
- Create: `app/src/test/java/com/lfr/dynamicforms/domain/usecase/EvaluateVisibilityUseCaseTest.kt`
- Create: `app/src/test/java/com/lfr/dynamicforms/domain/usecase/ValidatePageUseCaseTest.kt`

**Step 1: Create EvaluateVisibilityUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EvaluateVisibilityUseCaseTest {

    private lateinit var useCase: EvaluateVisibilityUseCase

    @Before
    fun setup() {
        useCase = EvaluateVisibilityUseCase()
    }

    @Test
    fun `element without visibleWhen is always visible`() {
        val element = TextFieldElement(id = "name", label = "Name")
        assertTrue(useCase.isVisible(element, emptyMap()))
    }

    @Test
    fun `equals operator matches field value`() {
        val element = TextFieldElement(
            id = "company",
            label = "Company",
            visibleWhen = VisibilityCondition("role", ConditionOperator.EQUALS, JsonPrimitive("dev"))
        )
        assertTrue(useCase.isVisible(element, mapOf("role" to "dev")))
        assertFalse(useCase.isVisible(element, mapOf("role" to "design")))
    }

    @Test
    fun `not_equals operator works`() {
        val element = TextFieldElement(
            id = "other",
            label = "Other",
            visibleWhen = VisibilityCondition("role", ConditionOperator.NOT_EQUALS, JsonPrimitive("admin"))
        )
        assertTrue(useCase.isVisible(element, mapOf("role" to "dev")))
        assertFalse(useCase.isVisible(element, mapOf("role" to "admin")))
    }

    @Test
    fun `greater_than operator compares numbers`() {
        val element = TextFieldElement(
            id = "senior_info",
            label = "Senior Info",
            visibleWhen = VisibilityCondition("experience", ConditionOperator.GREATER_THAN, JsonPrimitive(5))
        )
        assertTrue(useCase.isVisible(element, mapOf("experience" to "10")))
        assertFalse(useCase.isVisible(element, mapOf("experience" to "3")))
        assertFalse(useCase.isVisible(element, mapOf("experience" to "5")))
    }

    @Test
    fun `is_empty and is_not_empty operators work`() {
        val emptyCheck = TextFieldElement(
            id = "a", label = "A",
            visibleWhen = VisibilityCondition("field", ConditionOperator.IS_EMPTY, JsonPrimitive(""))
        )
        assertTrue(useCase.isVisible(emptyCheck, mapOf("field" to "")))
        assertFalse(useCase.isVisible(emptyCheck, mapOf("field" to "x")))

        val notEmptyCheck = TextFieldElement(
            id = "b", label = "B",
            visibleWhen = VisibilityCondition("field", ConditionOperator.IS_NOT_EMPTY, JsonPrimitive(""))
        )
        assertTrue(useCase.isVisible(notEmptyCheck, mapOf("field" to "x")))
        assertFalse(useCase.isVisible(notEmptyCheck, mapOf("field" to "")))
    }

    @Test
    fun `missing field treated as empty string`() {
        val element = TextFieldElement(
            id = "x", label = "X",
            visibleWhen = VisibilityCondition("missing", ConditionOperator.EQUALS, JsonPrimitive("val"))
        )
        assertFalse(useCase.isVisible(element, emptyMap()))
    }

    @Test
    fun `getVisibleElements filters page elements`() {
        val page = Page(
            pageId = "p1",
            title = "Page 1",
            elements = listOf(
                TextFieldElement(id = "always", label = "Always"),
                TextFieldElement(
                    id = "conditional", label = "Conditional",
                    visibleWhen = VisibilityCondition("toggle", ConditionOperator.EQUALS, JsonPrimitive("true"))
                )
            )
        )
        val visible = useCase.getVisibleElements(page, mapOf("toggle" to "false"))
        assertEquals(1, visible.size)
        assertEquals("always", visible[0].id)
    }
}
```

**Step 2: Create ValidatePageUseCaseTest.kt**

```kotlin
package com.lfr.dynamicforms.domain.usecase

import com.lfr.dynamicforms.domain.model.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidatePageUseCaseTest {

    private lateinit var useCase: ValidatePageUseCase

    @Before
    fun setup() {
        useCase = ValidatePageUseCase(EvaluateVisibilityUseCase())
    }

    @Test
    fun `required field with empty value returns error`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(id = "name", label = "Name", required = true)
        ))
        val errors = useCase.validate(page, emptyMap())
        assertEquals("Name is required", errors["name"])
    }

    @Test
    fun `optional empty field returns no error`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(id = "bio", label = "Bio", required = false)
        ))
        val errors = useCase.validate(page, emptyMap())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `text minLength validation`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "name", label = "Name",
                validation = TextValidation(minLength = 3, errorMessage = "Too short")
            )
        ))
        val errors = useCase.validate(page, mapOf("name" to "ab"))
        assertEquals("Too short", errors["name"])

        val noErrors = useCase.validate(page, mapOf("name" to "abc"))
        assertTrue(noErrors.isEmpty())
    }

    @Test
    fun `number range validation`() {
        val page = Page("p1", "Page", listOf(
            NumberFieldElement(
                id = "age", label = "Age",
                validation = NumberValidation(min = 18.0, max = 120.0)
            )
        ))
        assertNotNull(useCase.validate(page, mapOf("age" to "10"))["age"])
        assertTrue(useCase.validate(page, mapOf("age" to "25")).isEmpty())
        assertNotNull(useCase.validate(page, mapOf("age" to "abc"))["age"])
    }

    @Test
    fun `hidden field is not validated`() {
        val page = Page("p1", "Page", listOf(
            TextFieldElement(
                id = "company", label = "Company", required = true,
                visibleWhen = VisibilityCondition("role", ConditionOperator.EQUALS, JsonPrimitive("dev"))
            )
        ))
        val errors = useCase.validate(page, mapOf("role" to "design"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `firstPageWithErrors returns correct index`() {
        val pages = listOf(
            Page("p1", "Page 1", listOf(TextFieldElement(id = "a", label = "A"))),
            Page("p2", "Page 2", listOf(TextFieldElement(id = "b", label = "B")))
        )
        assertEquals(1, useCase.firstPageWithErrors(pages, mapOf("b" to "error")))
        assertEquals(0, useCase.firstPageWithErrors(pages, mapOf("a" to "error")))
    }
}
```

**Step 3: Run tests**

Run: `./gradlew test`
Expected: All tests pass.

**Step 4: Commit**

```bash
git add app/src/test/java/com/lfr/dynamicforms/domain/
git commit -m "test: add unit tests for EvaluateVisibilityUseCase and ValidatePageUseCase"
```

---

## Task 6: Data Layer - Remote

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/data/remote/FormApi.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/data/remote/MockInterceptor.kt`

**Step 1: Create FormApi.kt**

```kotlin
package com.lfr.dynamicforms.data.remote

import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FormApi {
    @GET("forms")
    suspend fun getForms(): List<FormSummary>

    @GET("forms/{formId}")
    suspend fun getForm(@Path("formId") formId: String): Form

    @POST("forms/{formId}/submit")
    suspend fun submitForm(
        @Path("formId") formId: String,
        @Body submission: FormSubmission
    ): SubmissionResponse
}
```

**Step 2: Create MockInterceptor.kt**

This provides hardcoded JSON responses so the app works without a real backend. Create the file at `app/src/main/java/com/lfr/dynamicforms/data/remote/MockInterceptor.kt`:

```kotlin
package com.lfr.dynamicforms.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val (code, json) = when {
            method == "GET" && path == "/forms" -> 200 to FORM_LIST_JSON
            method == "GET" && path.matches(Regex("/forms/[^/]+")) -> {
                val formId = path.removePrefix("/forms/")
                200 to getFormJson(formId)
            }
            method == "POST" && path.matches(Regex("/forms/[^/]+/submit")) -> 200 to SUBMIT_SUCCESS_JSON
            else -> 404 to """{"error":"not found"}"""
        }

        return Response.Builder()
            .code(code)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun getFormJson(formId: String): String = when (formId) {
        "registration_v1" -> REGISTRATION_FORM_JSON
        "feedback_v1" -> FEEDBACK_FORM_JSON
        else -> REGISTRATION_FORM_JSON
    }
}

private val FORM_LIST_JSON = """
[
  {"formId":"registration_v1","title":"User Registration","description":"Register a new account"},
  {"formId":"feedback_v1","title":"Feedback Form","description":"Share your feedback"}
]
""".trimIndent()

private val SUBMIT_SUCCESS_JSON = """
{"success":true,"message":"Form submitted successfully"}
""".trimIndent()

private val REGISTRATION_FORM_JSON = """
{
  "formId": "registration_v1",
  "title": "User Registration",
  "pages": [
    {
      "pageId": "page_1",
      "title": "Personal Info",
      "elements": [
        {
          "type": "section_header",
          "id": "header_personal",
          "label": "Personal Information",
          "subtitle": "Tell us about yourself"
        },
        {
          "type": "text_field",
          "id": "full_name",
          "label": "Full Name",
          "required": true,
          "validation": {
            "minLength": 2,
            "maxLength": 100,
            "errorMessage": "Name must be 2-100 characters"
          }
        },
        {
          "type": "number_field",
          "id": "age",
          "label": "Age",
          "required": true,
          "validation": {"min": 18, "max": 120}
        },
        {
          "type": "date_picker",
          "id": "birth_date",
          "label": "Date of Birth"
        },
        {
          "type": "dropdown",
          "id": "role",
          "label": "Role",
          "required": true,
          "options": [
            {"value": "dev", "label": "Developer"},
            {"value": "design", "label": "Designer"},
            {"value": "pm", "label": "Product Manager"}
          ]
        },
        {
          "type": "text_field",
          "id": "company_name",
          "label": "Company Name",
          "visibleWhen": {"fieldId": "role", "operator": "equals", "value": "dev"}
        }
      ]
    },
    {
      "pageId": "page_2",
      "title": "Preferences",
      "elements": [
        {
          "type": "toggle",
          "id": "newsletter",
          "label": "Subscribe to newsletter",
          "defaultValue": true
        },
        {
          "type": "slider",
          "id": "experience",
          "label": "Years of experience",
          "min": 0,
          "max": 30,
          "step": 1
        },
        {
          "type": "radio",
          "id": "contact_pref",
          "label": "Preferred contact method",
          "required": true,
          "options": [
            {"value": "email", "label": "Email"},
            {"value": "phone", "label": "Phone"},
            {"value": "none", "label": "No contact"}
          ]
        },
        {
          "type": "multi_select",
          "id": "interests",
          "label": "Interests",
          "options": [
            {"value": "android", "label": "Android"},
            {"value": "ios", "label": "iOS"},
            {"value": "web", "label": "Web"},
            {"value": "backend", "label": "Backend"}
          ]
        },
        {
          "type": "checkbox",
          "id": "terms",
          "label": "I agree to the terms and conditions",
          "required": true
        }
      ]
    },
    {
      "pageId": "page_3",
      "title": "Additional",
      "elements": [
        {
          "type": "text_field",
          "id": "bio",
          "label": "Bio",
          "multiline": true
        },
        {
          "type": "label",
          "id": "upload_info",
          "label": "Attachments",
          "text": "Upload your resume and sign below."
        },
        {
          "type": "file_upload",
          "id": "resume",
          "label": "Upload Resume",
          "allowedTypes": ["pdf", "doc", "docx"],
          "maxFileSize": 5242880
        },
        {
          "type": "signature",
          "id": "signature",
          "label": "Signature",
          "required": true
        }
      ]
    }
  ]
}
""".trimIndent()

private val FEEDBACK_FORM_JSON = """
{
  "formId": "feedback_v1",
  "title": "Feedback Form",
  "pages": [
    {
      "pageId": "fb_page_1",
      "title": "Your Feedback",
      "elements": [
        {
          "type": "section_header",
          "id": "fb_header",
          "label": "We value your feedback"
        },
        {
          "type": "slider",
          "id": "rating",
          "label": "Overall Rating",
          "min": 1,
          "max": 5,
          "step": 1,
          "required": true
        },
        {
          "type": "text_field",
          "id": "comments",
          "label": "Comments",
          "multiline": true
        },
        {
          "type": "radio",
          "id": "recommend",
          "label": "Would you recommend us?",
          "required": true,
          "options": [
            {"value": "yes", "label": "Yes"},
            {"value": "no", "label": "No"},
            {"value": "maybe", "label": "Maybe"}
          ]
        }
      ]
    }
  ]
}
""".trimIndent()
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/data/remote/
git commit -m "feat: add Retrofit FormApi and MockInterceptor with sample form data"
```

---

## Task 7: Data Layer - Local (Room)

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/data/local/DraftEntity.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/data/local/DraftDao.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/data/local/AppDatabase.kt`

**Step 1: Create DraftEntity.kt**

```kotlin
package com.lfr.dynamicforms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val formId: String,
    val pageIndex: Int,
    val valuesJson: String,
    val updatedAt: Long
)
```

**Step 2: Create DraftDao.kt**

```kotlin
package com.lfr.dynamicforms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE formId = :formId")
    suspend fun getDraft(formId: String): DraftEntity?

    @Query("DELETE FROM drafts WHERE formId = :formId")
    suspend fun deleteDraft(formId: String)

    @Query("SELECT formId FROM drafts")
    suspend fun getFormIdsWithDrafts(): List<String>
}
```

**Step 3: Create AppDatabase.kt**

```kotlin
package com.lfr.dynamicforms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DraftEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/data/local/
git commit -m "feat: add Room database with DraftEntity and DraftDao"
```

---

## Task 8: Data Layer - Repository Implementations

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/data/repository/FormRepositoryImpl.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/data/repository/DraftRepositoryImpl.kt`

**Step 1: Create FormRepositoryImpl.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormSubmission
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.SubmissionResponse
import com.lfr.dynamicforms.domain.repository.FormRepository
import javax.inject.Inject

class FormRepositoryImpl @Inject constructor(
    private val api: FormApi
) : FormRepository {

    override suspend fun getForms(): List<FormSummary> = api.getForms()

    override suspend fun getForm(formId: String): Form = api.getForm(formId)

    override suspend fun submitForm(formId: String, values: Map<String, String>): SubmissionResponse {
        return api.submitForm(formId, FormSubmission(formId, values))
    }
}
```

**Step 2: Create DraftRepositoryImpl.kt**

```kotlin
package com.lfr.dynamicforms.data.repository

import com.lfr.dynamicforms.data.local.DraftDao
import com.lfr.dynamicforms.data.local.DraftEntity
import com.lfr.dynamicforms.domain.model.Draft
import com.lfr.dynamicforms.domain.repository.DraftRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao
) : DraftRepository {

    private val json = Json

    override suspend fun saveDraft(formId: String, pageIndex: Int, values: Map<String, String>) {
        draftDao.upsert(
            DraftEntity(
                formId = formId,
                pageIndex = pageIndex,
                valuesJson = json.encodeToString(values),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getDraft(formId: String): Draft? {
        val entity = draftDao.getDraft(formId) ?: return null
        return Draft(
            formId = entity.formId,
            pageIndex = entity.pageIndex,
            values = json.decodeFromString(entity.valuesJson),
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun deleteDraft(formId: String) {
        draftDao.deleteDraft(formId)
    }

    override suspend fun getFormIdsWithDrafts(): List<String> {
        return draftDao.getFormIdsWithDrafts()
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/data/repository/
git commit -m "feat: add FormRepositoryImpl and DraftRepositoryImpl"
```

---

## Task 9: DI Modules + Hilt Application

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/di/NetworkModule.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/di/RepositoryModule.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/DynamicFormsApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Create NetworkModule.kt**

```kotlin
package com.lfr.dynamicforms.di

import com.lfr.dynamicforms.data.remote.FormApi
import com.lfr.dynamicforms.data.remote.MockInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(MockInterceptor())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.dynamicforms.mock/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideFormApi(retrofit: Retrofit): FormApi = retrofit.create(FormApi::class.java)
}
```

**Step 2: Create DatabaseModule.kt**

```kotlin
package com.lfr.dynamicforms.di

import android.content.Context
import androidx.room.Room
import com.lfr.dynamicforms.data.local.AppDatabase
import com.lfr.dynamicforms.data.local.DraftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dynamicforms.db").build()

    @Provides
    fun provideDraftDao(database: AppDatabase): DraftDao = database.draftDao()
}
```

**Step 3: Create RepositoryModule.kt**

```kotlin
package com.lfr.dynamicforms.di

import com.lfr.dynamicforms.data.repository.DraftRepositoryImpl
import com.lfr.dynamicforms.data.repository.FormRepositoryImpl
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFormRepository(impl: FormRepositoryImpl): FormRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(impl: DraftRepositoryImpl): DraftRepository
}
```

**Step 4: Create DynamicFormsApp.kt**

```kotlin
package com.lfr.dynamicforms

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DynamicFormsApp : Application()
```

**Step 5: Update AndroidManifest.xml**

Add `android:name=".DynamicFormsApp"` to the `<application>` tag:

```xml
<application
    android:name=".DynamicFormsApp"
    android:allowBackup="true"
    ...
```

**Step 6: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/di/ app/src/main/java/com/lfr/dynamicforms/DynamicFormsApp.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add Hilt DI modules, Application class, and manifest config"
```

---

## Task 10: Navigation Routes

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/navigation/Routes.kt`

**Step 1: Create Routes.kt**

```kotlin
package com.lfr.dynamicforms.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object FormListRoute

@Serializable
data class FormWizardRoute(val formId: String)

@Serializable
data class FormSuccessRoute(val formId: String)
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/presentation/navigation/
git commit -m "feat: add type-safe navigation route definitions"
```

---

## Task 11: Form Element Composables

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicTextField.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicNumberField.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicDropdown.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicCheckbox.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicRadioGroup.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicDatePicker.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicToggle.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicSlider.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicMultiSelect.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicFileUpload.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicSignature.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicSectionHeader.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicLabel.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/DynamicRepeatingGroup.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/elements/FormElementRenderer.kt`

**Step 1: Create DynamicTextField.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.TextFieldElement

@Composable
fun DynamicTextField(
    element: TextFieldElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (element.required) "${element.label} *" else element.label) },
            isError = error != null,
            minLines = if (element.multiline) 3 else 1,
            maxLines = if (element.multiline) 6 else 1,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 2: Create DynamicNumberField.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.lfr.dynamicforms.domain.model.NumberFieldElement

@Composable
fun DynamicNumberField(
    element: NumberFieldElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("-?\\d*\\.?\\d*"))) {
                    onValueChange(newValue)
                }
            },
            label = { Text(if (element.required) "${element.label} *" else element.label) },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 3: Create DynamicDropdown.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.DropdownElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDropdown(
    element: DropdownElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = element.options.find { it.value == value }?.label ?: ""

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (element.required) "${element.label} *" else element.label) },
                isError = error != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                element.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onValueChange(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 4: Create DynamicCheckbox.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.CheckboxElement

@Composable
fun DynamicCheckbox(
    element: CheckboxElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = value.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onValueChange(it.toString()) }
            )
            Text(if (element.required) "${element.label} *" else element.label)
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 5: Create DynamicRadioGroup.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.RadioElement

@Composable
fun DynamicRadioGroup(
    element: RadioElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
            style = MaterialTheme.typography.bodyLarge
        )
        element.options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = value == option.value,
                        onClick = { onValueChange(option.value) }
                    )
            ) {
                RadioButton(selected = value == option.value, onClick = { onValueChange(option.value) })
                Text(option.label)
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 6: Create DynamicDatePicker.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.DatePickerElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDatePicker(
    element: DatePickerElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (element.required) "${element.label} *" else element.label) },
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                LaunchedEffect(it) {
                    it.interactions.collect { interaction ->
                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                            showDialog = true
                        }
                    }
                }
            }
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = SimpleDateFormat(element.format, Locale.getDefault()).format(Date(millis))
                        onValueChange(formatted)
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

**Step 7: Create DynamicToggle.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.ToggleElement

@Composable
fun DynamicToggle(
    element: ToggleElement,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(element.label, modifier = Modifier.weight(1f))
        Switch(
            checked = value.toBooleanStrictOrNull() ?: element.defaultValue,
            onCheckedChange = { onValueChange(it.toString()) }
        )
    }
}
```

**Step 8: Create DynamicSlider.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.SliderElement
import kotlin.math.roundToInt

@Composable
fun DynamicSlider(
    element: SliderElement,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = value.toFloatOrNull() ?: element.min
    val steps = if (element.step > 0) ((element.max - element.min) / element.step).roundToInt() - 1 else 0

    Column(modifier = modifier.fillMaxWidth()) {
        Text("${element.label}: ${currentValue.roundToInt()}", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = currentValue,
            onValueChange = { onValueChange(it.toString()) },
            valueRange = element.min..element.max,
            steps = steps.coerceAtLeast(0)
        )
    }
}
```

**Step 9: Create DynamicMultiSelect.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.MultiSelectElement

@Composable
fun DynamicMultiSelect(
    element: MultiSelectElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = if (value.isBlank()) emptySet() else value.split(",").toSet()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
            style = MaterialTheme.typography.bodyLarge
        )
        element.options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = option.value in selected,
                    onCheckedChange = { checked ->
                        val updated = if (checked) selected + option.value else selected - option.value
                        onValueChange(updated.joinToString(","))
                    }
                )
                Text(option.label)
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 10: Create DynamicFileUpload.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.FileUploadElement

@Composable
fun DynamicFileUpload(
    element: FileUploadElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedButton(onClick = { /* TODO: launch file picker */ }) {
            Text(if (value.isBlank()) "Choose file" else value)
        }
        if (element.allowedTypes.isNotEmpty()) {
            Text(
                "Allowed: ${element.allowedTypes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 11: Create DynamicSignature.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SignatureElement

@Composable
fun DynamicSignature(
    element: SignatureElement,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (element.required) "${element.label} *" else element.label,
            style = MaterialTheme.typography.bodyLarge
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (value.isBlank()) {
                Text("Tap to sign", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Signed", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = { onValueChange("") }) { Text("Clear") }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Step 12: Create DynamicSectionHeader.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.SectionHeaderElement

@Composable
fun DynamicSectionHeader(
    element: SectionHeaderElement,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(element.label, style = MaterialTheme.typography.titleLarge)
        if (!element.subtitle.isNullOrBlank()) {
            Text(element.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

**Step 13: Create DynamicLabel.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.LabelElement

@Composable
fun DynamicLabel(
    element: LabelElement,
    modifier: Modifier = Modifier
) {
    Text(
        text = element.text.ifBlank { element.label },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth()
    )
}
```

**Step 14: Create DynamicRepeatingGroup.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement

@Composable
fun DynamicRepeatingGroup(
    element: RepeatingGroupElement,
    values: Map<String, String>,
    errors: Map<String, String>,
    itemCount: Int,
    onValueChange: (String, String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(element.label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        for (row in 0 until itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Item ${row + 1}", style = MaterialTheme.typography.labelLarge)
                        if (itemCount > element.minItems) {
                            IconButton(onClick = { onRemoveRow(row) }) {
                                Icon(Icons.Default.Delete, "Remove")
                            }
                        }
                    }
                    element.elements.forEach { childElement ->
                        val key = "${element.id}[$row].${childElement.id}"
                        FormElementRenderer(
                            element = childElement,
                            value = values[key] ?: "",
                            error = errors[key],
                            values = values,
                            errors = errors,
                            onValueChange = { onValueChange(key, it) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (itemCount < element.maxItems) {
            OutlinedButton(onClick = onAddRow, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add ${element.label}")
            }
        }
    }
}
```

**Step 15: Create FormElementRenderer.kt**

```kotlin
package com.lfr.dynamicforms.presentation.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfr.dynamicforms.domain.model.*

@Composable
fun FormElementRenderer(
    element: FormElement,
    value: String,
    error: String?,
    values: Map<String, String>,
    errors: Map<String, String>,
    onValueChange: (String) -> Unit,
    onRepeatingGroupValueChange: (String, String) -> Unit = { _, _ -> },
    onAddRow: (String) -> Unit = {},
    onRemoveRow: (String, Int) -> Unit = { _, _ -> },
    repeatingGroupCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    when (element) {
        is TextFieldElement -> DynamicTextField(element, value, error, onValueChange, modifier)
        is NumberFieldElement -> DynamicNumberField(element, value, error, onValueChange, modifier)
        is DropdownElement -> DynamicDropdown(element, value, error, onValueChange, modifier)
        is CheckboxElement -> DynamicCheckbox(element, value, error, onValueChange, modifier)
        is RadioElement -> DynamicRadioGroup(element, value, error, onValueChange, modifier)
        is DatePickerElement -> DynamicDatePicker(element, value, error, onValueChange, modifier)
        is ToggleElement -> DynamicToggle(element, value, onValueChange, modifier)
        is SliderElement -> DynamicSlider(element, value, onValueChange, modifier)
        is MultiSelectElement -> DynamicMultiSelect(element, value, error, onValueChange, modifier)
        is FileUploadElement -> DynamicFileUpload(element, value, error, onValueChange, modifier)
        is SignatureElement -> DynamicSignature(element, value, error, onValueChange, modifier)
        is SectionHeaderElement -> DynamicSectionHeader(element, modifier)
        is LabelElement -> DynamicLabel(element, modifier)
        is RepeatingGroupElement -> DynamicRepeatingGroup(
            element = element,
            values = values,
            errors = errors,
            itemCount = repeatingGroupCounts[element.id] ?: element.minItems,
            onValueChange = onRepeatingGroupValueChange,
            onAddRow = { onAddRow(element.id) },
            onRemoveRow = { row -> onRemoveRow(element.id, row) },
            modifier = modifier
        )
    }
}
```

**Step 16: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 17: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/presentation/elements/
git commit -m "feat: add all form element composables and FormElementRenderer"
```

---

## Task 12: Form MVI + ViewModel + Screen

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormAction.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormUiState.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormEffect.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormViewModel.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormScreen.kt`

**Step 1: Create FormAction.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

sealed class FormAction {
    data class LoadForm(val formId: String) : FormAction()
    data class UpdateField(val fieldId: String, val value: String) : FormAction()
    data object NextPage : FormAction()
    data object PrevPage : FormAction()
    data object Submit : FormAction()
    data object SaveDraft : FormAction()
    data class AddRepeatingRow(val groupId: String) : FormAction()
    data class RemoveRepeatingRow(val groupId: String, val rowIndex: Int) : FormAction()
}
```

**Step 2: Create FormUiState.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

import com.lfr.dynamicforms.domain.model.Form

data class FormUiState(
    val isLoading: Boolean = true,
    val form: Form? = null,
    val currentPageIndex: Int = 0,
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val repeatingGroupCounts: Map<String, Int> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val totalPages: Int get() = form?.pages?.size ?: 0
    val isFirstPage: Boolean get() = currentPageIndex == 0
    val isLastPage: Boolean get() = currentPageIndex == totalPages - 1
    val currentPage get() = form?.pages?.getOrNull(currentPageIndex)
    val progressFraction: Float get() = if (totalPages > 0) (currentPageIndex + 1f) / totalPages else 0f
}
```

**Step 3: Create FormEffect.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

sealed class FormEffect {
    data class NavigateToSuccess(val formId: String, val message: String) : FormEffect()
    data class ShowError(val message: String) : FormEffect()
    data object DraftSaved : FormEffect()
}
```

**Step 4: Create FormViewModel.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    private val getForm: GetFormUseCase,
    private val saveDraft: SaveDraftUseCase,
    private val submitForm: SubmitFormUseCase,
    private val validatePage: ValidatePageUseCase,
    private val evaluateVisibility: EvaluateVisibilityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FormUiState())
    val state: StateFlow<FormUiState> = _state.asStateFlow()

    private val _effect = Channel<FormEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: FormAction) {
        when (action) {
            is FormAction.LoadForm -> loadForm(action.formId)
            is FormAction.UpdateField -> updateField(action.fieldId, action.value)
            is FormAction.NextPage -> nextPage()
            is FormAction.PrevPage -> prevPage()
            is FormAction.Submit -> submit()
            is FormAction.SaveDraft -> saveDraftNow()
            is FormAction.AddRepeatingRow -> addRow(action.groupId)
            is FormAction.RemoveRepeatingRow -> removeRow(action.groupId, action.rowIndex)
        }
    }

    private fun loadForm(formId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = getForm(formId)
                val groupCounts = mutableMapOf<String, Int>()
                for (page in result.form.pages) {
                    for (element in page.elements) {
                        if (element is RepeatingGroupElement) {
                            groupCounts[element.id] = element.minItems
                        }
                    }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        form = result.form,
                        values = result.initialValues,
                        currentPageIndex = result.initialPageIndex,
                        repeatingGroupCounts = groupCounts
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load form") }
            }
        }
    }

    private fun updateField(fieldId: String, value: String) {
        _state.update { current ->
            val newValues = current.values + (fieldId to value)
            val newErrors = current.errors - fieldId
            current.copy(values = newValues, errors = newErrors)
        }
    }

    private fun nextPage() {
        val current = _state.value
        val page = current.currentPage ?: return
        val errors = validatePage.validate(page, current.values)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = it.errors + errors) }
            return
        }
        _state.update { it.copy(currentPageIndex = it.currentPageIndex + 1, errors = emptyMap()) }
        saveDraftNow()
    }

    private fun prevPage() {
        _state.update { it.copy(currentPageIndex = (it.currentPageIndex - 1).coerceAtLeast(0)) }
        saveDraftNow()
    }

    private fun submit() {
        val current = _state.value
        val form = current.form ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                when (val result = submitForm(form, current.values)) {
                    is SubmitResult.Success -> {
                        _state.update { it.copy(isSubmitting = false) }
                        _effect.send(FormEffect.NavigateToSuccess(form.formId, result.message))
                    }
                    is SubmitResult.ValidationFailed -> {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errors = result.errors,
                                currentPageIndex = result.firstErrorPage
                            )
                        }
                    }
                    is SubmitResult.ServerError -> {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errors = result.fieldErrors,
                                currentPageIndex = result.firstErrorPage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false) }
                _effect.send(FormEffect.ShowError(e.message ?: "Submission failed"))
            }
        }
    }

    private fun saveDraftNow() {
        val current = _state.value
        val formId = current.form?.formId ?: return
        viewModelScope.launch {
            try {
                saveDraft(formId, current.currentPageIndex, current.values)
            } catch (_: Exception) { }
        }
    }

    private fun addRow(groupId: String) {
        _state.update { current ->
            val counts = current.repeatingGroupCounts.toMutableMap()
            counts[groupId] = (counts[groupId] ?: 1) + 1
            current.copy(repeatingGroupCounts = counts)
        }
    }

    private fun removeRow(groupId: String, rowIndex: Int) {
        _state.update { current ->
            val counts = current.repeatingGroupCounts.toMutableMap()
            val count = counts[groupId] ?: return@update current
            counts[groupId] = (count - 1).coerceAtLeast(0)

            val newValues = current.values.toMutableMap()
            val prefix = "$groupId[$rowIndex]."
            newValues.keys.removeAll { it.startsWith(prefix) }

            for (i in (rowIndex + 1) until count) {
                val oldPrefix = "$groupId[$i]."
                val newPrefix = "$groupId[${i - 1}]."
                val keysToShift = newValues.keys.filter { it.startsWith(oldPrefix) }
                for (key in keysToShift) {
                    val value = newValues.remove(key) ?: continue
                    newValues[key.replaceFirst(oldPrefix, newPrefix)] = value
                }
            }

            current.copy(values = newValues, repeatingGroupCounts = counts)
        }
    }
}
```

**Step 5: Create FormScreen.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lfr.dynamicforms.domain.model.FormElement
import com.lfr.dynamicforms.presentation.elements.FormElementRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSuccess: (String, String) -> Unit,
    viewModel: FormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(formId) {
        viewModel.onAction(FormAction.LoadForm(formId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FormEffect.NavigateToSuccess -> onNavigateToSuccess(effect.formId, effect.message)
                is FormEffect.ShowError -> { /* handled via snackbar or state */ }
                is FormEffect.DraftSaved -> { }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.currentPage?.title ?: state.form?.title ?: "Form") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!state.isFirstPage) viewModel.onAction(FormAction.PrevPage)
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.totalPages > 1) {
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Step ${state.currentPageIndex + 1} of ${state.totalPages}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        state.currentPage?.elements?.forEach { element ->
                            FormElementRenderer(
                                element = element,
                                value = state.values[element.id] ?: "",
                                error = state.errors[element.id],
                                values = state.values,
                                errors = state.errors,
                                onValueChange = { viewModel.onAction(FormAction.UpdateField(element.id, it)) },
                                onRepeatingGroupValueChange = { key, value ->
                                    viewModel.onAction(FormAction.UpdateField(key, value))
                                },
                                onAddRow = { viewModel.onAction(FormAction.AddRepeatingRow(it)) },
                                onRemoveRow = { id, row -> viewModel.onAction(FormAction.RemoveRepeatingRow(id, row)) },
                                repeatingGroupCounts = state.repeatingGroupCounts
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!state.isFirstPage) {
                            OutlinedButton(onClick = { viewModel.onAction(FormAction.PrevPage) }) {
                                Text("Back")
                            }
                        } else {
                            Spacer(Modifier)
                        }

                        if (state.isLastPage) {
                            Button(
                                onClick = { viewModel.onAction(FormAction.Submit) },
                                enabled = !state.isSubmitting
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Submit")
                            }
                        } else {
                            Button(onClick = { viewModel.onAction(FormAction.NextPage) }) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**Step 6: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/presentation/form/
git commit -m "feat: add Form MVI (Action/State/Effect), ViewModel, and FormScreen"
```

---

## Task 13: Form List Screen

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/list/FormListViewModel.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/list/FormListScreen.kt`

**Step 1: Create FormListViewModel.kt**

```kotlin
package com.lfr.dynamicforms.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormListState(
    val isLoading: Boolean = true,
    val forms: List<FormSummary> = emptyList(),
    val drafts: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class FormListViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FormListState())
    val state: StateFlow<FormListState> = _state.asStateFlow()

    init {
        loadForms()
    }

    fun refresh() = loadForms()

    private fun loadForms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val forms = formRepository.getForms()
                val drafts = draftRepository.getFormIdsWithDrafts().toSet()
                _state.update { it.copy(isLoading = false, forms = forms, drafts = drafts) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load forms") }
            }
        }
    }
}
```

**Step 2: Create FormListScreen.kt**

```kotlin
package com.lfr.dynamicforms.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreen(
    onFormClick: (String) -> Unit,
    viewModel: FormListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dynamic Forms") }) }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.forms, key = { it.formId }) { form ->
                        val hasDraft = form.formId in state.drafts
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onFormClick(form.formId) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(form.title, style = MaterialTheme.typography.titleMedium)
                                    if (hasDraft) {
                                        AssistChip(
                                            onClick = { onFormClick(form.formId) },
                                            label = { Text("Resume") }
                                        )
                                    }
                                }
                                if (form.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        form.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/presentation/list/
git commit -m "feat: add FormListViewModel and FormListScreen with draft resume indicator"
```

---

## Task 14: Navigation + MainActivity Wiring

**Files:**
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/navigation/AppNavHost.kt`
- Create: `app/src/main/java/com/lfr/dynamicforms/presentation/form/FormSuccessScreen.kt`
- Modify: `app/src/main/java/com/lfr/dynamicforms/MainActivity.kt`

**Step 1: Create FormSuccessScreen.kt**

```kotlin
package com.lfr.dynamicforms.presentation.form

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormSuccessScreen(
    message: String,
    onBackToList: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Success!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBackToList) {
                Text("Back to Forms")
            }
        }
    }
}
```

**Step 2: Create AppNavHost.kt**

```kotlin
package com.lfr.dynamicforms.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lfr.dynamicforms.presentation.form.FormScreen
import com.lfr.dynamicforms.presentation.form.FormSuccessScreen
import com.lfr.dynamicforms.presentation.list.FormListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = FormListRoute) {
        composable<FormListRoute> {
            FormListScreen(
                onFormClick = { formId -> navController.navigate(FormWizardRoute(formId)) }
            )
        }
        composable<FormWizardRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FormWizardRoute>()
            FormScreen(
                formId = route.formId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSuccess = { formId, message ->
                    navController.navigate(FormSuccessRoute(formId)) {
                        popUpTo(FormListRoute) { inclusive = false }
                    }
                }
            )
        }
        composable<FormSuccessRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FormSuccessRoute>()
            FormSuccessScreen(
                message = "Form submitted successfully",
                onBackToList = {
                    navController.popBackStack(FormListRoute, inclusive = false)
                }
            )
        }
    }
}
```

**Step 3: Update MainActivity.kt**

```kotlin
package com.lfr.dynamicforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lfr.dynamicforms.presentation.navigation.AppNavHost
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicFormsTheme {
                AppNavHost()
            }
        }
    }
}
```

**Step 4: Verify full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/lfr/dynamicforms/presentation/navigation/ app/src/main/java/com/lfr/dynamicforms/presentation/form/FormSuccessScreen.kt app/src/main/java/com/lfr/dynamicforms/MainActivity.kt
git commit -m "feat: add navigation, success screen, and wire up MainActivity"
```

---

## Task 15: Run Tests + Final Verification

**Step 1: Run unit tests**

Run: `./gradlew test`
Expected: All tests pass.

**Step 2: Build full APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL with APK at `app/build/outputs/apk/debug/`.

**Step 3: Delete boilerplate test**

Remove the default `ExampleUnitTest.kt` if it conflicts or is no longer relevant.

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: final cleanup and verification"
```
