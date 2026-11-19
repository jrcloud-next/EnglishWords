package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.jr.englishword.net.Api
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
        snackbarHost = { EditorialSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            EditorialTopBar(
                title = "单词本",
                onBack = onBack,
                badge = {
                    InfoPill(
                        text = "${words.size} 词",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                trailing = {
                    if (words.isNotEmpty()) {
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

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("搜索单词、词性或释义", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清除", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(Modifier.height(10.dp))

            when {
                words.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradientIconChip(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 56.dp,
                            iconSize = 28.dp,
                            cornerRadius = 17.dp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("词库为空", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "先去「导入单词」添加一些单词吧",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradientIconChip(
                            icon = Icons.Rounded.Search,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 52.dp,
                            iconSize = 24.dp,
                            cornerRadius = 16.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("未找到匹配的单词", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "请尝试更换关键词或清除搜索条件",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val groupedWords = filtered
                        .sortedBy { it.word.lowercase() }
                        .groupBy { it.word.firstOrNull()?.uppercase() ?: "#" }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedWords.forEach { (letter, wordsInGroup) ->
                            item(key = "header_$letter") {
                                AlphabetSectionHeader(letter = letter, count = wordsInGroup.size)
                            }
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
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("清空词库", fontWeight = FontWeight.Bold) },
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
        WordDetailSheet(
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (word.pos.isNotBlank()) {
                        Spacer(Modifier.width(7.dp))
                        PosBadge(pos = word.pos, fontSize = 11.sp)
                    }
                    if (word.practiced > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(7.dp)
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
                Spacer(Modifier.height(3.dp))
                Text(
                    text = word.meaning,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordDetailSheet(
    word: WordEntry,
    apiEnabled: Boolean,
    vm: AppViewModel,
    onDismiss: () -> Unit
) {
    var aiLoading by remember(word.id) { mutableStateOf(false) }
    var aiResult by remember(word.id) { mutableStateOf<Api.AiWordInfo?>(null) }
    var aiError by remember(word.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun fetchAiInfo() {
        aiLoading = true
        aiError = null
        scope.launch {
            when (val r = vm.moreInfo(word)) {
                is Api.AiInfoResult.Ok -> aiResult = r.info
                is Api.AiInfoResult.Err -> aiError = r.message
            }
            aiLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.word,
                    fontFamily = EditorialSerif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (word.practiced > 0) {
                    val masteryPct = (word.mastery * 100).toInt()
                    InfoPill(
                        text = "练习 ${word.practiced} 次 · $masteryPct%",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (word.pos.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                PosBadge(pos = word.pos, fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = word.meaning,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            if (!apiEnabled) {
                Text(
                    text = "可在「设置」中开启 AI 扩展功能，查看该词的更多义项和例句。",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (aiLoading) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "正在生成详细释义与例句…",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                aiError?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "请求失败：$err",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (aiResult == null && !aiLoading) {
                    if (aiError == null) {
                        Button(
                            onClick = { fetchAiInfo() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI 更多释义")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { fetchAiInfo() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("重试 AI 释义")
                        }
                    }
                }

                aiResult?.let { info ->
                    if (info.senses.isNotEmpty()) {
                        Text(
                            text = "更多义项",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        info.senses.forEachIndexed { idx, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontFamily = EditorialSerif,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(20.dp)
                                )
                                if (s.pos.isNotBlank()) {
                                    PosBadge(pos = s.pos, fontSize = 11.sp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = s.meaning,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (info.example.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(14.dp)
                            ) {
                                Box(
                                    Modifier
                                        .width(2.5.dp)
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = info.example,
                                        fontFamily = EditorialSerif,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (info.exampleCn.isNotBlank()) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            text = info.exampleCn,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
