package com.creditrepairai.v2

import android.os.Bundle
import android.view.WindowManager
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
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            CreditRepairTheme {
                CreditRepairApp(viewModel)
            }
        }
    }
}
