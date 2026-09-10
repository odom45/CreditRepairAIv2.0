package com.creditrepairai.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.creditrepairai.v2.ui.CreditRepairApp
import com.creditrepairai.v2.ui.theme.CreditRepairTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreditRepairTheme {
                CreditRepairApp(viewModel)
            }
        }
    }
}
