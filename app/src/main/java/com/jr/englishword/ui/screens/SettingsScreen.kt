package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.jr.englishword.data.AppSettings
import com.jr.englishword.net.DeepSeekApi
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.SectionCardHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, toast: (String) -> Unit, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var baseUrl by rememberSaveable { mutableStateOf(settings.baseUrl) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    // 整页统一为 surface（浅色下即纯白），避免顶部栏与页面底色不一致
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "返回", modifier = Modifier.size(18.dp))
            }
            Text("设置", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // AI 扩展功能
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Text(
                        "四选一干扰项由 AI 生成（需完成下方配置，未配置时自动从词库随机选取）；单词本内可查看 AI 详细释义与例句。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(14.dp))

                    Text("API 地址", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        enabled = settings.apiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例如：https://api.deepseek.com", fontSize = 12.sp) },
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("模型名称", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        enabled = settings.apiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例如：deepseek-flash", fontSize = 12.sp) },
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "API Key",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(14.dp))

                    // 保存配置按钮
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("保存配置")
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
                                    testResult = when (val r = DeepSeekApi.testConnection(testSettings)) {
                                        is DeepSeekApi.ApiResult.Ok -> "✓ 连接成功（${r.latencyMs} ms）"
                                        is DeepSeekApi.ApiResult.Err -> "✗ ${r.message}"
                                    }
                                    testing = false
                                }
                            },
                            enabled = settings.apiEnabled && !testing,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (testing) "测试中…" else "测试连接")
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
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("清空配置")
                        }
                    }
                    testResult?.let { r ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            r,
                            fontSize = 13.sp,
                            color = if (r.startsWith("✓")) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 学习设置
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionCardHeader(
                        icon = Icons.Rounded.School,
                        title = "学习设置",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "每轮练习抽取的题目数量，错题重练同样按此数量出题。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { n ->
                            FilterChip(
                                selected = settings.questionsPerRound == n,
                                onClick = { vm.updateSettings(settings.copy(questionsPerRound = n)) },
                                label = { Text("$n", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 关于
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionCardHeader(
                        icon = Icons.Rounded.Info,
                        title = "关于",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "英语单词·By JR v1.1\n支持 TXT / DOCX 词表导入，提供英译中、中译英、默写中文、拼写英文四种记忆模式，内置错题本，并可通过 AI 接口生成选择题干扰项与详细释义。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
