package com.lfr.dynamicforms.presentation.form

sealed class FormEffect {
    data class NavigateToSuccess(val formId: String, val message: String) : FormEffect()
    data class ShowError(val message: String) : FormEffect()
    data object DraftSaved : FormEffect()
}
