package com.example.quickcalculation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickcalculation.data.ThemeRepository
import com.example.quickcalculation.ui.QuestionScreen
import com.example.quickcalculation.ui.TypeSelectScreen
import com.example.quickcalculation.ui.theme.QuickCalculationTheme
import com.example.quickcalculation.ui.theme.ThemeMode
import com.example.quickcalculation.viewmodel.QuizViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeRepo = ThemeRepository(applicationContext)
        setContent {
            val themeMode by themeRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            QuickCalculationTheme(themeMode = themeMode) {
                val vm: QuizViewModel = viewModel()
                val nav = rememberNavController()
                val scope = rememberCoroutineScope()
                NavHost(navController = nav, startDestination = "type-select") {
                    composable("type-select") {
                        TypeSelectScreen(vm = vm, themeMode = themeMode,
                            onThemeModeChange = { m -> scope.launch { themeRepo.setThemeMode(m) } },
                            onStart = { nav.navigate("question") })
                    }
                    composable("question") {
                        QuestionScreen(vm = vm, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
