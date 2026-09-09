package com.example.quickcalculation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.ui.theme.ThemeMode
import com.example.quickcalculation.viewmodel.AnswerMode
import com.example.quickcalculation.viewmodel.QuizViewModel

@Composable
fun TypeSelectScreen(
    vm: QuizViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onStart: () -> Unit,
) {
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("选择题型", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        QuestionType.entries.forEach { type ->
            FilterChip(
                selected = type in state.selectedTypes,
                onClick = { vm.toggleType(type) },
                label = { Text(type.label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("难度", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = state.difficulty == d, onClick = { vm.setDifficulty(d) },
                    label = { Text(difficultyLabel(d)) })
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("作答模式", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.answerMode == AnswerMode.CHOICE,
                onClick = { vm.setAnswerMode(AnswerMode.CHOICE) }, label = { Text("选择") })
            FilterChip(selected = state.answerMode == AnswerMode.FILL,
                onClick = { vm.setAnswerMode(AnswerMode.FILL) }, label = { Text("填空") })
        }
        Spacer(Modifier.height(24.dp))
        Text("主题", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(selected = themeMode == m, onClick = { onThemeModeChange(m) },
                    label = { Text(themeModeLabel(m)) })
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { vm.startQuiz(); onStart() },
            enabled = state.selectedTypes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("开始练习") }
    }
}

private fun difficultyLabel(d: Difficulty) = when (d) {
    Difficulty.EASY -> "简单"
    Difficulty.MEDIUM -> "中等"
    Difficulty.HARD -> "困难"
}
private fun themeModeLabel(m: ThemeMode) = when (m) {
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
    ThemeMode.SYSTEM -> "跟随系统"
}
