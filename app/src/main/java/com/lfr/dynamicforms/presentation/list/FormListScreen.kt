package com.lfr.dynamicforms.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreen(
    onFormClick: (String) -> Unit,
    viewModel: FormListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FormListScreenContent(state = state, onFormClick = onFormClick, onRetry = { viewModel.refresh() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreenContent(
    state: FormListState,
    onFormClick: (String) -> Unit,
    onRetry: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { TopAppBar(title = { Text("Dynamic Forms") }) }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.forms, key = { it.formId }) { form ->
                        val hasDraft = form.formId in state.drafts
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_card_${form.formId}")
                                .clickable { onFormClick(form.formId) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = form.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (form.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        form.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FormBadge(
                                        text = "${form.pageCount} steps",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    FormBadge(
                                        text = "${form.fieldCount} fields",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    if (hasDraft) {
                                        FormBadge(
                                            text = "Resume",
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
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
}

@Preview(name = "Form List", showBackground = true)
@Composable
private fun FormListScreenContentPreview() {
    val state = FormListState(
        isLoading = false,
        forms = listOf(
            FormSummary("f1", "Safety Inspection", "Monthly workplace safety checklist", 3, 12),
            FormSummary("f2", "Employee Onboarding", "New hire registration form", 5, 24),
        ),
        drafts = setOf("f2"),
    )
    DynamicFormsTheme(dynamicColor = false) {
        FormListScreenContent(state = state, onFormClick = {})
    }
}

@Composable
private fun FormBadge(
    text: String,
    containerColor: Color,
    contentColor: Color = Color.Unspecified
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
