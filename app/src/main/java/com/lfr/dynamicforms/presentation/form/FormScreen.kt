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
