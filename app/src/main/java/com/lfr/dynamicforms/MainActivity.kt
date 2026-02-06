package com.lfr.dynamicforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lfr.dynamicforms.presentation.navigation.AppNavHost
import com.lfr.dynamicforms.ui.theme.DynamicFormsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicFormsTheme {
                AppNavHost()
            }
        }
    }
}
