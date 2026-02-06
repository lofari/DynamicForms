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
