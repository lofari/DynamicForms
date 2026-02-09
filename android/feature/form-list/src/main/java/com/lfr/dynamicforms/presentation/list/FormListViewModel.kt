package com.lfr.dynamicforms.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.FormSummary
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.repository.DraftRepository
import com.lfr.dynamicforms.domain.repository.FormRepository
import com.lfr.dynamicforms.domain.repository.SubmissionQueueRepository
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

sealed interface FormListEffect {
    data class ShowError(val message: String) : FormListEffect
}

@HiltViewModel
class FormListViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val draftRepository: DraftRepository,
    private val submissionQueueRepository: SubmissionQueueRepository,
    private val syncWorkScheduler: SyncWorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(FormListState())
    val state: StateFlow<FormListState> = _state.asStateFlow()

    private val _effect = Channel<FormListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadForms()
        observePendingSubmissions()
    }

    fun refresh() = loadForms()

    fun retrySubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.retry(id).fold(
                onSuccess = { syncWorkScheduler.scheduleSync() },
                onFailure = { error -> _effect.send(FormListEffect.ShowError(error.toUserMessage())) }
            )
        }
    }

    fun discardSubmission(id: String) {
        viewModelScope.launch {
            submissionQueueRepository.delete(id).fold(
                onSuccess = { },
                onFailure = { error -> _effect.send(FormListEffect.ShowError(error.toUserMessage())) }
            )
        }
    }

    private fun loadForms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            formRepository.getForms().fold(
                onSuccess = { forms ->
                    draftRepository.getFormIdsWithDrafts().fold(
                        onSuccess = { drafts ->
                            _state.update { it.copy(isLoading = false, forms = forms, drafts = drafts.toSet()) }
                        },
                        onFailure = {
                            _state.update { it.copy(isLoading = false, forms = forms, drafts = emptySet()) }
                        }
                    )
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
            )
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
