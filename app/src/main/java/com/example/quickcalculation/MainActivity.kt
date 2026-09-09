package com.example.quickcalculation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.quickcalculation.data.ThemeRepository
import com.example.quickcalculation.ui.theme.QuickCalculationTheme
import com.example.quickcalculation.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeRepo = ThemeRepository(applicationContext)
        setContent {
            val themeMode by themeRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            QuickCalculationTheme(themeMode = themeMode) {
                // NavHost 在 Task 4 补齐；此处先放占位
            }
        }
    }
}
