package com.lfr.dynamicforms.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object FormListRoute

@Serializable
data class FormWizardRoute(val formId: String)

@Serializable
data class FormSuccessRoute(val formId: String)
