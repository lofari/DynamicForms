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
