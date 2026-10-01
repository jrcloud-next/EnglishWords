package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
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
import com.jr.englishword.data.WordEntry
import com.jr.englishword.net.DeepSeekApi
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.GradientIconChip
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.components.staggeredAppear
import com.jr.englishword.ui.components.InfoPill
import kotlinx.coroutines.launch

@Composable
fun ListScreen(vm: AppViewModel, toast: (String) -> Unit, onBack: () -> Unit) {
    val words by vm.words.collectAsState()
    val settings by vm.settings.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var detailWord by remember { mutableStateOf<WordEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteWord(w: WordEntry) {
        val current = vm.words.value
        val idx = current.indexOfFirst { it.id == w.id }
        if (idx < 0) return
        // 先记下会被联动删除的错题记录，撤销时按原值还原
        val removedWrong = vm.wrongRecordOf(w.id)
        vm.updateWords { list -> list.filterNot { it.id == w.id } }
        vm.removeWrong(w.id)
        scope.launch {
            val r = snackbarHostState.showSnackbar(
                message = "已删除「${w.word}」",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) {
                vm.updateWords { list ->
                    list.toMutableList().apply { add(idx.coerceAtMost(size), w) }
                }
                removedWrong?.let { vm.restoreWrong(it) }
            }
        }
    }

    val filtered = if (query.isBlank()) words else words.filter {
        it.word.contains(query, ignoreCase = true) ||
            it.meaning.contains(query, ignoreCase = true) ||
            it.pos.contains(query, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 外层 Scaffold 已处理窗口 insets，内层不再重复添加
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "返回", modifier = Modifier.size(18.dp))
                }
                Text("单词本", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                InfoPill(
                    text = "${words.size} 词",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))
                if (words.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("搜索单词 / 释义") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清除", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            Spacer(Modifier.height(10.dp))

            if (words.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradientIconChip(
                            icon = Icons.Rounded.MenuBook,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 60.dp,
                            iconSize = 30.dp,
                            cornerRadius = 20.dp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("词库为空", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "先去「导入单词」添加一些单词吧",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 按首字母分组排序
                val groupedWords = filtered
                    .sortedBy { it.word.lowercase() }
                    .groupBy { it.word.firstOrNull()?.uppercase() ?: "#" }

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedWords.forEach { (letter, wordsInGroup) ->
                        // 首字母标签
                        item(key = "header_$letter") {
                            Column(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                                InfoPill(
                                    text = letter,
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        // 该字母下的单词
                        itemsIndexed(wordsInGroup, key = { _, w -> w.id }) { i, w ->
                            WordRow(
                                word = w,
                                index = i,
                                onDelete = { deleteWord(w) },
                                onClick = { detailWord = w }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空词库") },
            text = { Text("将删除全部 ${words.size} 个单词及其学习记录，且无法恢复。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.updateWords { emptyList() }
                    vm.clearWrong()
                    toast("词库已清空")
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    detailWord?.let { w ->
        WordDetailDialog(
            word = w,
            apiEnabled = settings.apiEnabled,
            vm = vm,
            onDismiss = { detailWord = null }
        )
    }
}

@Composable
private fun WordRow(
    word: WordEntry,
    index: Int,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index, stepMillis = 30)
            .pressableScale(source),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        word.word,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (word.pos.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(word.pos, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (word.practiced > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        word.mastery >= 0.75f -> MaterialTheme.colorScheme.secondary
                                        word.mastery >= 0.4f -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    CircleShape
                                )
                            )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    word.meaning,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WordDetailDialog(
    word: WordEntry,
    apiEnabled: Boolean,
    vm: AppViewModel,
    onDismiss: () -> Unit
) {
    var aiLoading by remember(word.id) { mutableStateOf(false) }
    var aiResult by remember(word.id) { mutableStateOf<DeepSeekApi.AiWordInfo?>(null) }
    var aiError by remember(word.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(word.word, fontWeight = FontWeight.Bold)
                if (word.pos.isNotBlank()) {
                    Text(word.pos, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(word.meaning, fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))

                if (!apiEnabled) {
                    Text(
                        "可在「设置」中开启 AI 扩展功能，查看该词的更多义项和例句。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (aiResult == null && !aiLoading && aiError == null) {
                        Button(
                            onClick = {
                                aiLoading = true
                                aiError = null
                                scope.launch {
                                    when (val r = vm.moreInfo(word)) {
                                        is DeepSeekApi.AiInfoResult.Ok -> aiResult = r.info
                                        is DeepSeekApi.AiInfoResult.Err -> aiError = r.message
                                    }
                                    aiLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI 更多释义")
                        }
                    }
                    if (aiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("正在请求 AI…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    aiError?.let { err ->
                        Text("请求失败：$err", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    aiResult?.let { info ->
                        if (info.senses.isNotEmpty()) {
                            Text("更多义项", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            info.senses.forEach { s ->
                                Row(Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        if (s.pos.isNotBlank()) "${s.pos} " else "",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(s.meaning, fontSize = 14.sp)
                                }
                            }
                        }
                        if (info.example.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(info.example, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    if (info.exampleCn.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            info.exampleCn,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
