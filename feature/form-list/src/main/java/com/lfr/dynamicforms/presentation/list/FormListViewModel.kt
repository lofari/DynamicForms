package com.lfr.dynamicforms.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lfr.dynamicforms.presentation.util.toUserMessage
import javax.inject.Inject

data class FormListState(
    val isLoading: Boolean = true,
    val forms: List<FormSummary> = emptyList(),
    val drafts: Set<String> = emptySet(),
    val pendingSubmissions: List<PendingSubmission> = emptyList(),
    val pendingCountsByFormId: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class FormListViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val syncWorkScheduler: SyncWorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(FormListState())
    val state: StateFlow<FormListState> = _state.asStateFlow()

    init {
        loadForms()
        observePendingSubmissions()
    }

    fun refresh() = loadForms()

    fun retrySubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.retry(id)
            syncWorkScheduler.scheduleSync()
        }
    }

    fun discardSubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.delete(id)
        }
    }

    private fun loadForms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val forms = formRepository.getForms()
                val drafts = draftRepository.getFormIdsWithDrafts().toSet()
                _state.update { it.copy(isLoading = false, forms = forms, drafts = drafts) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    private fun observePendingSubmissions() {
        viewModelScope.launch {
            submissionQueueRepository.observeAll().collect { submissions ->
                val countsByForm = submissions.groupBy { it.formId }
                    .mapValues { it.value.size }
                _state.update {
                    it.copy(
                        pendingSubmissions = submissions,
                        pendingCountsByFormId = countsByForm
                    )
                }
            }
        }
    }
}
