package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.QuizMode
import com.jr.englishword.data.WordEntry
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.AlphabetSectionHeader
import com.jr.englishword.ui.components.EditorialSnackbarHost
import com.jr.englishword.ui.components.EditorialTopBar
import com.jr.englishword.ui.components.GradientIconChip
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.PosBadge
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.components.staggeredAppear
import com.jr.englishword.ui.theme.EditorialSerif
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongBookScreen(
    vm: AppViewModel,
    toast: (String) -> Unit,
    onBack: () -> Unit,
    onStartQuiz: (QuizMode, List<WordEntry>) -> Unit
) {
    val words by vm.words.collectAsState()
    val wrongRecords by vm.wrong.collectAsState()
    val settings by vm.settings.collectAsState()

    var selectedMode by rememberSaveable { mutableStateOf(QuizMode.EN_CN) }
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 词库中可能已删除部分单词，仅展示仍存在的错题
    val resolved = wrongRecords
        .mapNotNull { r -> words.find { it.id == r.wordId }?.let { w -> w to r } }
        .sortedByDescending { it.second.lastWrongAt }
    val roundSize = minOf(settings.questionsPerRound, resolved.size)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { EditorialSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            EditorialTopBar(
                title = "错题本",
                onBack = onBack,
                badge = {
                    InfoPill(
                        text = "${resolved.size} 词",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                },
                trailing = {
                    if (resolved.isNotEmpty()) {
                        Surface(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = "清空",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 书卷纸卡风重练控制面板（取代原有的刺眼三色渐变块）
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredAppear(0),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GradientIconChip(
                                icon = Icons.AutoMirrored.Rounded.Assignment,
                                tint = MaterialTheme.colorScheme.tertiary,
                                size = 42.dp,
                                iconSize = 21.dp,
                                cornerRadius = 13.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "错题重练",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (resolved.isEmpty()) "答错的单词会自动收进错题本"
                                    else "随机抽取 $roundSize 题，答对即移出错题本",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (resolved.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "选择重练模式",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                QuizMode.entries.forEach { m ->
                                    val isSelected = selectedMode == m
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMode = m },
                                        label = {
                                            Text(
                                                text = m.title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        ),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onStartQuiz(selectedMode, resolved.map { it.first }) },
                            enabled = resolved.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (resolved.isEmpty()) "错题本为空" else "开始重练（$roundSize 题）",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (resolved.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GradientIconChip(
                                icon = Icons.AutoMirrored.Rounded.Assignment,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 56.dp,
                                iconSize = 28.dp,
                                cornerRadius = 17.dp
                            )
                            Spacer(Modifier.height(14.dp))
                            Text("错题本是空的", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "练习中答错的单词会自动收进这里",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val groupedWords = resolved
                        .sortedBy { it.first.word.lowercase() }
                        .groupBy { it.first.word.firstOrNull()?.uppercase() ?: "#" }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedWords.forEach { (letter, group) ->
                            item(key = "header_$letter") {
                                AlphabetSectionHeader(letter = letter, count = group.size)
                            }
                            itemsIndexed(group, key = { _, item -> item.first.id }) { i, (w, r) ->
                                WrongWordRow(
                                    word = w,
                                    count = r.count,
                                    lastWrongAt = r.lastWrongAt,
                                    index = i,
                                    onRemove = {
                                        vm.removeWrong(w.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "已将「${w.word}」移出错题本",
                                                actionLabel = "撤销",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                // 按原值还原，不重置错误次数与最近答错时间
                                                vm.restoreWrong(r)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("清空错题本", fontWeight = FontWeight.Bold) },
            text = { Text("将移除全部 ${resolved.size} 条错题记录，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearWrong()
                    toast("错题本已清空")
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun WrongWordRow(
    word: WordEntry,
    count: Int,
    lastWrongAt: Long,
    index: Int,
    onRemove: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index, stepMillis = 25)
            .pressableScale(source),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        fontFamily = EditorialSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (word.pos.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        PosBadge(pos = word.pos, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(7.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "错 ${count} 次",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = word.meaning,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lastWrongAt > 0) {
                    Spacer(Modifier.height(2.dp))
                    val formattedTime = remember(lastWrongAt) {
                        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(lastWrongAt))
                    }
                    Text(
                        text = "最近答错：$formattedTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "移出",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
