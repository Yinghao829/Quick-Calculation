package com.example.quickcalculation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quickcalculation.ui.theme.QuickColors
import com.example.quickcalculation.viewmodel.AnswerMode
import com.example.quickcalculation.viewmodel.QuizViewModel

@Composable
fun QuestionScreen(vm: QuizViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val q = state.question
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("返回") }
            Text("已答 ${state.stats.answered} · 正确率 ${accuracyText(state.stats.accuracy)}",
                style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        if (q != null) {
            Text("${q.type.label} · ${q.subType}", style = MaterialTheme.typography.labelLarge,
                color = QuickColors.PrimaryLight)
            Spacer(Modifier.height(12.dp))
            Text(q.stem, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            if (state.answerMode == AnswerMode.CHOICE) {
                q.options.forEachIndexed { idx, opt ->
                    val isSelected = state.selectedOption == idx
                    val isCorrectOption = opt == q.correctAnswer
                    val showGreen = state.answered && isCorrectOption
                    val showRed = state.answered && isSelected && !isCorrectOption
                    OptionButton(
                        text = AnswerFormatter.format(opt, q.unit),
                        selected = isSelected,
                        green = showGreen,
                        red = showRed,
                        onClick = { vm.selectOption(idx) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                OutlinedTextField(
                    value = state.fillInput,
                    onValueChange = { vm.setFillInput(it) },
                    label = { Text("输入答案（${q.unit}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.fillInput.isNotBlank() && state.fillInput.toDoubleOrNull() == null,
                    supportingText = {
                        if (state.fillInput.isNotBlank() && state.fillInput.toDoubleOrNull() == null) {
                            Text("请输入数字", color = QuickColors.Error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.submitFill(state.fillInput) },
                    enabled = !state.answered && state.fillInput.isNotBlank(),
                ) { Text("确认") }
            }
            if (state.answered) {
                Spacer(Modifier.height(16.dp))
                ResultBar(
                    correct = state.isCorrect == true,
                    answer = AnswerFormatter.format(q.correctAnswer, q.unit),
                    explanation = q.explanation,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.nextQuestion() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("下一题")
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    selected: Boolean,
    green: Boolean,
    red: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        green -> QuickColors.Success
        red -> QuickColors.Error
        selected -> QuickColors.PrimaryLight
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        green -> QuickColors.Success.copy(alpha = 0.15f)
        red -> QuickColors.Error.copy(alpha = 0.15f)
        selected -> QuickColors.PrimaryLight.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val borderWidth = if (green || red || selected) 2.dp else 1.dp
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (green) Text("✓", color = QuickColors.Success)
            if (red) Text("✗", color = QuickColors.Error)
        }
    }
}

@Composable
private fun ResultBar(correct: Boolean, answer: String, explanation: String) {
    val color = if (correct) QuickColors.Success else QuickColors.Error
    val mark = if (correct) "✓" else "✗"
    val markText = if (correct) "回答正确" else "回答错误"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(
            "$mark $markText",
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "正确答案：$answer",
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun accuracyText(a: Float): String = "${(a * 100).toInt()}%"
