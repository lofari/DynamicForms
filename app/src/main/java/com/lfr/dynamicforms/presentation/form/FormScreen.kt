package com.lfr.dynamicforms.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.FormElement
import com.lfr.dynamicforms.domain.model.Page
import com.lfr.dynamicforms.domain.model.SectionHeaderElement
import com.lfr.dynamicforms.domain.model.TextFieldElement
import com.lfr.dynamicforms.presentation.elements.FormElementRenderer
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * Groups a flat list of form elements into sections, splitting at each [SectionHeaderElement].
 * Elements that appear before the first section header are placed in a group with a null header.
 */
private data class SectionGroup(
    val header: SectionHeaderElement?,
    val elements: List<FormElement>
)

private fun groupElementsIntoSections(elements: List<FormElement>): List<SectionGroup> {
    val groups = mutableListOf<SectionGroup>()
    var currentHeader: SectionHeaderElement? = null
    var currentElements = mutableListOf<FormElement>()

    for (element in elements) {
        if (element is SectionHeaderElement) {
            // Flush the previous group (even if it has no elements, skip empty headerless groups)
            if (currentHeader != null || currentElements.isNotEmpty()) {
                groups.add(SectionGroup(currentHeader, currentElements))
            }
            currentHeader = element
            currentElements = mutableListOf()
        } else {
            currentElements.add(element)
        }
    }

    // Flush the last group
    if (currentHeader != null || currentElements.isNotEmpty()) {
        groups.add(SectionGroup(currentHeader, currentElements))
    }

    return groups
}

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

    FormScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreenContent(
    state: FormUiState,
    onAction: (FormAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                title = { Text(state.currentPage?.title ?: state.form?.title ?: "Form") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!state.isFirstPage) onAction(FormAction.PrevPage)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("progress_text")
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
                        val sections = groupElementsIntoSections(
                            state.currentPage?.elements ?: emptyList()
                        )

                        sections.forEach { section ->
                            ElevatedCard(
                                elevation = CardDefaults.elevatedCardElevation(),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (section.header != null) {
                                        Column(
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = section.header.label,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (section.header.subtitle != null) {
                                                Text(
                                                    text = section.header.subtitle!!,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    section.elements.forEach { element ->
                                        FormElementRenderer(
                                            element = element,
                                            value = state.values[element.id] ?: "",
                                            error = state.errors[element.id],
                                            values = state.values,
                                            errors = state.errors,
                                            onValueChange = { onAction(FormAction.UpdateField(element.id, it)) },
                                            onRepeatingGroupValueChange = { key, value ->
                                                onAction(FormAction.UpdateField(key, value))
                                            },
                                            onAddRow = { onAction(FormAction.AddRepeatingRow(it)) },
                                            onRemoveRow = { id, row -> onAction(FormAction.RemoveRepeatingRow(id, row)) },
                                            repeatingGroupCounts = state.repeatingGroupCounts
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!state.isFirstPage) {
                            OutlinedButton(onClick = { onAction(FormAction.PrevPage) }, modifier = Modifier.testTag("btn_back")) {
                                Text("Back")
                            }
                        } else {
                            Spacer(Modifier)
                        }

                        if (state.isLastPage) {
                            Button(
                                onClick = { onAction(FormAction.Submit) },
                                modifier = Modifier.testTag("btn_submit"),
                                enabled = !state.isSubmitting
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Submit")
                            }
                        } else {
                            Button(onClick = { onAction(FormAction.NextPage) }, modifier = Modifier.testTag("btn_next")) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Form - Loaded", showBackground = true)
@Composable
private fun FormScreenContentPreview() {
    val samplePage = Page(
        pageId = "p1",
        title = "Personal Information",
        elements = listOf(
            SectionHeaderElement(id = "sh1", label = "Contact Details", subtitle = "Fill in your details"),
            TextFieldElement(id = "name", label = "Full Name", required = true),
            TextFieldElement(id = "email", label = "Email", required = true),
        )
    )
    val state = FormUiState(
        isLoading = false,
        form = Form(formId = "f1", title = "Registration", pages = listOf(samplePage, samplePage)),
        currentPageIndex = 0,
        values = mapOf("name" to "Jane Doe"),
        errors = mapOf("email" to "Email is required"),
    )
    DynamicFormsTheme(dynamicColor = false) {
        FormScreenContent(state = state, onAction = {}, onNavigateBack = {})
    }
}
