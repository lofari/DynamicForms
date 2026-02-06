package com.lfr.dynamicforms.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lfr.dynamicforms.presentation.form.FormScreen
import com.lfr.dynamicforms.presentation.form.FormSuccessScreen
import com.lfr.dynamicforms.presentation.list.FormListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = FormListRoute) {
        composable<FormListRoute> {
            FormListScreen(
                onFormClick = { formId -> navController.navigate(FormWizardRoute(formId)) }
            )
        }
        composable<FormWizardRoute> {
            FormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSuccess = { formId, message ->
                    navController.navigate(FormSuccessRoute(formId, message)) {
                        popUpTo(FormListRoute) { inclusive = false }
                    }
                }
            )
        }
        composable<FormSuccessRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FormSuccessRoute>()
            FormSuccessScreen(
                message = route.message,
                onBackToList = {
                    navController.popBackStack(FormListRoute, inclusive = false)
                }
            )
        }
    }
}
