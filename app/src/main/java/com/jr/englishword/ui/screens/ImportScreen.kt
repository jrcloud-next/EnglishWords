package com.jr.englishword.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.Parser
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.EditorialTopBar
import com.jr.englishword.ui.components.GradientIconChip
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.PosBadge
import com.jr.englishword.ui.components.SegmentedTabs
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.theme.EditorialSerif
import com.jr.englishword.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun ImportScreen(
    vm: AppViewModel,
    toast: (String) -> Unit,
    goList: () -> Unit,
    onBack: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var rawInput by rememberSaveable { mutableStateOf("") }
    var parsed by remember { mutableStateOf<Parser.ParseResult?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var replace by rememberSaveable { mutableStateOf(false) }
    var importedMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        loading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("无法读取文件")
                    val name = resolveFileName(context, uri)
                    val isDocx = name.endsWith(".docx", ignoreCase = true) ||
                        (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
                    val text = if (isDocx) Parser.extractDocxText(bytes) else Parser.decodeBytes(bytes)
                    name to Parser.parseText(text)
                }
                fileName = result.first
                parsed = result.second
                if (result.second.words.isEmpty()) toast("没有解析出任何单词，请检查文件格式")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("解析失败：${e.message}")
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        EditorialTopBar(title = "导入单词", onBack = onBack)

        SegmentedTabs(
            tabs = listOf("文件导入（TXT / DOCX）", "简单导入"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(120))) },
            label = "importTab"
        ) { t ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (t == 0) {
                    FileImportContent(
                        fileName = fileName,
                        loading = loading,
                        onPick = { picker.launch(arrayOf("*/*")) }
                    )
                } else {
                    SimpleImportContent(
                        rawInput = rawInput,
                        onInput = { rawInput = it },
                        onParse = {
                            val r = Parser.parseText(rawInput)
                            parsed = r
                            if (r.words.isEmpty()) toast("没有解析出任何单词，请检查格式")
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))
                parsed?.let { p ->
                    PreviewCard(
                        result = p,
                        replace = replace,
                        onReplaceChange = { replace = it },
                        onClear = { parsed = null; fileName = null },
                        onImport = {
                            val added = vm.addWords(p.words, replace)
                            importedMsg = "成功导入 $added 个单词" +
                                if (p.words.size - added > 0) "（跳过重复 ${p.words.size - added} 个）" else ""
                            parsed = null
                            rawInput = ""
                            fileName = null
                        }
                    )
                }

                importedMsg?.let { msg ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = appColors.successContainer),
                        border = BorderStroke(1.dp, appColors.success.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = appColors.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = msg,
                                fontSize = 14.sp,
                                color = appColors.onSuccessContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = goList) { Text("去单词本") }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FileImportContent(fileName: String?, loading: Boolean, onPick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Card(
        onClick = onPick,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(source),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientIconChip(
                icon = Icons.Rounded.CloudUpload,
                tint = MaterialTheme.colorScheme.primary,
                size = 62.dp,
                iconSize = 30.dp,
                cornerRadius = 18.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (loading) "正在解析文件…" else "点击选择 TXT 或 DOCX 文件",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            if (fileName != null) {
                InfoPill(
                    text = fileName,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "支持 “1. hello  n.  你好” 等词表格式",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleImportContent(rawInput: String, onInput: (String) -> Unit, onParse: () -> Unit) {
    OutlinedTextField(
        value = rawInput,
        onValueChange = onInput,
        modifier = Modifier.fillMaxWidth(),
        minLines = 7,
        placeholder = {
            Text(
                text = "每行一个单词，格式示例：\n1. hello  n.  你好\n2. complex  adj.  复杂的\nhello  n.  你好（序号与词性均可省略）",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onParse,
        enabled = rawInput.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = MaterialTheme.shapes.medium
    ) { Text("解析内容", fontSize = 15.sp) }
}

@Composable
private fun PreviewCard(
    result: Parser.ParseResult,
    replace: Boolean,
    onReplaceChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onImport: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "解析出 ${result.words.size} 个单词",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClear) { Text("清除") }
            }
            if (result.failedLines.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "有 ${result.failedLines.size} 行无法解析，例如：${result.failedLines.take(2).joinToString("；")}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            result.words.take(8).forEach { w ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = w.word,
                        fontFamily = EditorialSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (w.pos.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        PosBadge(pos = w.pos, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = w.meaning,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (result.words.size > 8) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "…等共 ${result.words.size} 个单词",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .toggleable(
                        value = replace,
                        role = Role.Checkbox,
                        onValueChange = onReplaceChange
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = replace, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "清空现有单词及其学习记录后导入",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onImport,
                enabled = result.words.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("导入 ${result.words.size} 个单词", fontSize = 15.sp) }
        }
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)?.let { return it }
                }
            }
    }
    return uri.lastPathSegment ?: "import"
}
