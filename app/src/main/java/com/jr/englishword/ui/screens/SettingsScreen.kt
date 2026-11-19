package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.net.AiUsageSnapshot
import com.jr.englishword.net.Api
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.EditorialTopBar
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.SectionCardHeader
import com.jr.englishword.ui.theme.EditorialSerif
import com.jr.englishword.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, toast: (String) -> Unit, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val lastBusinessUsage by vm.lastBusinessUsage.collectAsState()
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    var baseUrl by rememberSaveable { mutableStateOf(settings.baseUrl) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditorialTopBar(title = "设置", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // 1. AI 扩展功能
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionCardHeader(
                        icon = Icons.Rounded.Psychology,
                        title = "AI 扩展功能",
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        Switch(
                            checked = settings.apiEnabled,
                            onCheckedChange = { checked ->
                                vm.updateSettings(settings.copy(apiEnabled = checked))
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "四选一干扰项由 AI 生成（需完成下方配置，未配置时自动从词库随机选取）；单词本内可查看 AI 详细释义与例句。",
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(14.dp))

                    FieldLabel("API 地址")
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        enabled = settings.apiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例如：https://api.deepseek.com", fontSize = 13.sp) },
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))

                    FieldLabel("模型名称")
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        enabled = settings.apiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例如：deepseek-flash", fontSize = 13.sp) },
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))

                    FieldLabel("API Key")
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        enabled = settings.apiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showKey) "隐藏密钥" else "显示密钥",
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    baseUrl = baseUrl.trim(),
                                    model = model.trim(),
                                    apiKey = apiKey.trim()
                                )
                            )
                            toast("配置已保存")
                        },
                        enabled = settings.apiEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("保存配置", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                testing = true
                                testResult = null
                                scope.launch {
                                    val testSettings = settings.copy(
                                        baseUrl = baseUrl.trim(),
                                        model = model.trim(),
                                        apiKey = apiKey.trim()
                                    )
                                    testResult = when (val r = Api.testConnection(testSettings)) {
                                        is Api.ApiResult.Ok -> "✓ 连接成功（${r.latencyMs} ms）"
                                        is Api.ApiResult.Err -> "✗ ${r.message}"
                                    }
                                    testing = false
                                }
                            },
                            enabled = settings.apiEnabled && !testing,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(if (testing) "测试中…" else "测试连接", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                baseUrl = ""
                                model = ""
                                apiKey = ""
                                vm.updateSettings(settings.copy(baseUrl = "", model = "", apiKey = ""))
                                testResult = null
                                toast("配置已清空")
                            },
                            enabled = settings.apiEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("清空配置", fontSize = 13.sp)
                        }
                    }

                    testResult?.let { r ->
                        val isOk = r.startsWith("✓")
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOk) appColors.successContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = r,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                fontSize = 13.sp,
                                color = if (isOk) appColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(14.dp))
                    RecentAiUsage(lastBusinessUsage)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. 学习设置
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionCardHeader(
                        icon = Icons.Rounded.School,
                        title = "学习设置",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "每轮练习抽取的题目数量，错题重练同样按此数量出题。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { n ->
                            val selected = settings.questionsPerRound == n
                            Surface(
                                onClick = { vm.updateSettings(settings.copy(questionsPerRound = n)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "$n",
                                        fontFamily = EditorialSerif,
                                        fontSize = 15.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. 关于
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionCardHeader(
                        icon = Icons.Rounded.Info,
                        title = "关于",
                        tint = MaterialTheme.colorScheme.tertiary
                    ) {
                        InfoPill(
                            text = "v1.3",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "英语单词·By JR v1.3\n支持 TXT / DOCX 词表导入，提供英译中、中译英、默写中文、拼写英文四种记忆模式，内置错题本，并可通过 AI 接口生成选择题干扰项与详细释义。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
private fun RecentAiUsage(snapshot: AiUsageSnapshot?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "最近一次 AI 请求",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "仅统计最近一次成功的业务请求，记录在本次应用进程内保留；“测试连接”不计入。",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (snapshot == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无请求记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val completedAt = remember(snapshot.completedAtMs) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(snapshot.completedAtMs))
                }
                val usage = snapshot.usage
                val hitRate = usage?.cacheHitRate?.let {
                    String.format(Locale.getDefault(), "%.1f%%", it * 100.0)
                } ?: "服务未提供"

                val rows = listOf(
                    "任务" to snapshot.task.displayName,
                    "服务域名" to snapshot.serviceHost.ifBlank { "服务未提供" },
                    "模型" to snapshot.model.ifBlank { "服务未提供" },
                    "完成时间" to completedAt,
                    "输入 token" to tokenCount(usage?.promptTokens),
                    "输出 token" to tokenCount(usage?.completionTokens),
                    "缓存命中 token" to tokenCount(usage?.cacheHitTokens),
                    "缓存未命中 token" to tokenCount(usage?.cacheMissTokens),
                    "本次输入缓存命中率" to hitRate
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    rows.forEachIndexed { idx, (label, value) ->
                        val isLast = idx == rows.lastIndex
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = value,
                                fontSize = 12.sp,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isLast) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tokenCount(value: Long?): String = value?.toString() ?: "服务未提供"
