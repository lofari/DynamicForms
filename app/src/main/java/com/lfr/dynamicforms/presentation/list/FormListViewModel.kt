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
import com.lfr.dynamicforms.presentation.util.toUserMessage
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
                _state.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }
}
