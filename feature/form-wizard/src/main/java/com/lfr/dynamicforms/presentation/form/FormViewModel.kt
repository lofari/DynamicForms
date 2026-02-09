package com.lfr.dynamicforms.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.dynamicforms.domain.model.Form
import com.lfr.dynamicforms.domain.model.RepeatingGroupElement
import com.lfr.dynamicforms.domain.model.fold
import com.lfr.dynamicforms.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lfr.dynamicforms.presentation.util.toUserMessage
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getForm: GetFormUseCase,
    private val saveDraft: SaveDraftUseCase,
    @Suppress("unused") private val submitForm: SubmitFormUseCase,
    private val enqueueSubmission: EnqueueSubmissionUseCase,
    private val validatePage: ValidatePageUseCase,
    private val evaluateVisibility: EvaluateVisibilityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FormUiState())
    val state: StateFlow<FormUiState> = _state.asStateFlow()

    private val _effect = Channel<FormEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var draftDebounceJob: Job? = null

    companion object {
        private const val DRAFT_SAVE_DEBOUNCE_MS = 2000L
    }

    init {
        val formId = savedStateHandle.get<String>("formId")
        if (formId != null) loadForm(formId)
    }

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
            _state.update { it.copy(isLoading = true, errorMessage = null, formId = formId) }

            getForm(formId).fold(
                onSuccess = { result ->
                    val groupCounts = mutableMapOf<String, Int>()
                    for (page in result.form.pages) {
                        for (element in page.elements) {
                            if (element is RepeatingGroupElement) {
                                groupCounts[element.id] = element.minItems
                            }
                        }
                    }
                    val visibleIds = computeVisibleIds(result.form, result.initialValues)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            form = result.form,
                            values = result.initialValues,
                            currentPageIndex = result.initialPageIndex,
                            repeatingGroupCounts = groupCounts,
                            visibleElementIds = visibleIds
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
            )
        }
    }

    private fun updateField(fieldId: String, value: String) {
        _state.update { current ->
            val newValues = current.values + (fieldId to value)
            val newErrors = current.errors - fieldId
            val visibleIds = computeVisibleIds(current.form, newValues)
            current.copy(values = newValues, errors = newErrors, visibleElementIds = visibleIds)
        }
        scheduleDraftSave()
    }

    private fun computeVisibleIds(form: Form?, values: Map<String, String>): Set<String> {
        if (form == null) return emptySet()
        return form.pages.flatMap { page ->
            evaluateVisibility.getVisibleElements(page, values).map { it.id }
        }.toSet()
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

        // Client-side validation before enqueuing
        val errors = validatePage.validateAllPages(form.pages, current.values)
        if (errors.isNotEmpty()) {
            val firstPage = validatePage.firstPageWithErrors(form.pages, errors)
            _state.update {
                it.copy(errors = errors, currentPageIndex = firstPage)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                enqueueSubmission(form.formId, form.title, current.values)
                _state.update { it.copy(isSubmitting = false) }
                _effect.send(FormEffect.NavigateToSuccess(form.formId, "Form queued for submission"))
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false) }
                _effect.send(FormEffect.ShowError(e.toUserMessage()))
            }
        }
    }

    private fun scheduleDraftSave() {
        draftDebounceJob?.cancel()
        draftDebounceJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            saveDraftNow()
        }
    }

    override fun onCleared() {
        draftDebounceJob?.cancel()
        saveDraftNow()
    }

    private fun saveDraftNow() {
        val current = _state.value
        val formId = current.form?.formId ?: return
        viewModelScope.launch {
            try {
                saveDraft(formId, current.currentPageIndex, current.values)
            } catch (e: Exception) {
                Timber.w(e, "Draft save failed")
            }
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
