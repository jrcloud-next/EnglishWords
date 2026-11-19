package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.QuizMode
import com.jr.englishword.net.Api
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.AnimatedCounter
import com.jr.englishword.ui.components.GradientBar
import com.jr.englishword.ui.components.GradientIconChip
import com.jr.englishword.ui.components.IconChip
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.components.staggeredAppear
import com.jr.englishword.ui.theme.EditorialSerif
import com.jr.englishword.ui.theme.LocalAppColors

private fun modeIcon(mode: QuizMode): ImageVector = when (mode) {
    QuizMode.EN_CN -> Icons.Rounded.Translate
    QuizMode.CN_EN -> Icons.Rounded.Spellcheck
    QuizMode.DICTATION -> Icons.Rounded.EditNote
    QuizMode.SPELLING -> Icons.Rounded.Keyboard
}

private fun modeCaption(mode: QuizMode): String = when (mode) {
    QuizMode.EN_CN -> "看词选义"
    QuizMode.CN_EN -> "看义选词"
    QuizMode.DICTATION -> "看词写义"
    QuizMode.SPELLING -> "看义拼词"
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartQuiz: (QuizMode) -> Unit,
    goImport: () -> Unit,
    goList: () -> Unit,
    goWrongBook: () -> Unit,
    goSettings: () -> Unit,
    toast: (String) -> Unit
) {
    val words by vm.words.collectAsState()
    val wrongRecords by vm.wrong.collectAsState()
    val settings by vm.settings.collectAsState()
    val loadError by vm.loadError.collectAsState()
    val appColors = LocalAppColors.current
    val total = words.size
    val mastered = words.count { it.practiced > 0 && it.mastery >= 0.75f }
    val wordIds = words.mapTo(HashSet()) { it.id }
    val wrongCount = wrongRecords.count { it.wordId in wordIds }
    val isAiConfigured = Api.isReady(settings)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        HomeMasthead(aiEnabled = isAiConfigured, onSettingsClick = goSettings)
        Spacer(Modifier.height(16.dp))

        loadError?.let { msg ->
            LoadErrorBanner(msg, index = 0) { vm.clearLoadError() }
            Spacer(Modifier.height(14.dp))
        }

        OverviewCard(
            total = total,
            mastered = mastered,
            wrongCount = wrongCount,
            aiEnabled = isAiConfigured
        )
        Spacer(Modifier.height(26.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredAppear(1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择记忆模式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "每轮 ${settings.questionsPerRound} 题",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))

        if (total == 0) {
            EmptyBankCard(goImport)
            Spacer(Modifier.height(18.dp))
        } else {
            val modes = QuizMode.entries
            var index = 2
            for (row in modes.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (mode in row) {
                        ModeCard(
                            mode = mode,
                            accent = appColors.accent(mode.ordinal),
                            index = index++,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (total == 0) toast("词库为空，请先导入单词")
                                else onStartQuiz(mode)
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
        WrongBookBanner(wrongCount, 6, goWrongBook)
        Spacer(Modifier.height(22.dp))

        Text(
            text = "词库与设置",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.staggeredAppear(6)
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("导入单词", Icons.Rounded.UploadFile, appColors.accent(0), 7, Modifier.weight(1f), goImport)
            QuickAction("单词本", Icons.AutoMirrored.Rounded.MenuBook, appColors.accent(1), 8, Modifier.weight(1f), goList)
            QuickAction("设置", Icons.Rounded.Settings, appColors.accent(2), 9, Modifier.weight(1f), goSettings)
        }

        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Powered By JR Wang",
                fontFamily = EditorialSerif,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** 首页书卷刊头：左列标题，右列可直达设置的 AI 状态胶囊。 */
@Composable
private fun HomeMasthead(aiEnabled: Boolean, onSettingsClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(0),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "LEXICON · 词汇手账",
                fontFamily = EditorialSerif,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "英语单词",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (aiEnabled) appColors.success
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (aiEnabled) "AI 开启" else "AI 关闭",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** 首页概览纸卡：克制的白纸微边框容器，呈现三栏统计与掌握进度条。 */
@Composable
private fun OverviewCard(
    total: Int,
    mastered: Int,
    wrongCount: Int,
    aiEnabled: Boolean
) {
    val appColors = LocalAppColors.current
    val masteryRatio = if (total == 0) 0f else mastered.toFloat() / total
    val masteryPct = if (total == 0) 0 else (mastered * 100 / total)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(0),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverviewStatNumber(
                    label = "词库单词",
                    value = total,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OverviewVerticalRule()
                OverviewStatNumber(
                    label = "已掌握",
                    value = mastered,
                    valueColor = if (mastered > 0) appColors.success else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OverviewVerticalRule()
                OverviewStatText(
                    label = "AI 状态",
                    value = if (aiEnabled) "开启" else "关闭",
                    valueColor = if (aiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            if (total > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "词库掌握进度",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (wrongCount > 0) "$masteryPct% · 待巩固 $wrongCount 词" else "$masteryPct%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                GradientBar(
                    progress = masteryRatio,
                    stops = appColors.heroStops,
                    height = 5.dp
                )
            }
        }
    }
}

@Composable
private fun OverviewStatNumber(
    label: String,
    value: Int,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedCounter(
            value = value,
            style = TextStyle(
                fontFamily = EditorialSerif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            ),
            color = valueColor
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun OverviewStatText(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun OverviewVerticalRule() {
    Box(
        Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun ModeCard(
    mode: QuizMode,
    accent: Color,
    index: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier
            .staggeredAppear(index)
            .pressableScale(source),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 最小高度设置在内容层，确保无界滚动容器内上下排布均衡且支持大字号自适应伸展
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 126.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIconChip(
                    icon = modeIcon(mode),
                    tint = accent,
                    size = 40.dp,
                    iconSize = 20.dp,
                    cornerRadius = 12.dp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Column {
                Text(
                    text = mode.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = modeCaption(mode),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongBookBanner(count: Int, index: Int, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val accent = MaterialTheme.colorScheme.tertiary
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index)
            .pressableScale(source),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) MaterialTheme.colorScheme.surfaceContainerLowest
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            if (count > 0) accent.copy(alpha = 0.32f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIconChip(
                icon = Icons.AutoMirrored.Rounded.Assignment,
                tint = if (count > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 40.dp,
                iconSize = 20.dp,
                cornerRadius = 12.dp
            )
            Spacer(Modifier.width(12.dp))
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(Modifier.padding(end = 8.dp)) {
                    Text(
                        text = "错题本",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (count > 0) "$count 个单词答错过，点击巩固" else "暂无错题，继续保持",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (count > 0) {
                    InfoPill(
                        text = "去重练",
                        containerColor = accent.copy(alpha = 0.12f),
                        contentColor = accent,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            if (count == 0) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyBankCard(goImport: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    Card(
        onClick = goImport,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(1)
            .pressableScale(source),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientIconChip(
                icon = Icons.Rounded.UploadFile,
                tint = primary,
                size = 54.dp,
                iconSize = 26.dp,
                cornerRadius = 16.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "词库还是空的",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "导入 TXT / DOCX 词表或粘贴单词，开启日常记忆",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            InfoPill(
                text = "前往导入",
                containerColor = primary.copy(alpha = 0.10f),
                contentColor = primary
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    color: Color,
    index: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier
            .staggeredAppear(index)
            .pressableScale(source),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GradientIconChip(
                icon = icon,
                tint = color,
                size = 34.dp,
                iconSize = 17.dp,
                cornerRadius = 11.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** 数据文件损坏时的常驻提示：明确告知按空数据启动、原文件已尝试备份。 */
@Composable
private fun LoadErrorBanner(message: String, index: Int, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Rounded.ErrorOutline,
                    tint = MaterialTheme.colorScheme.error,
                    size = 32.dp,
                    iconSize = 17.dp,
                    cornerRadius = 10.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "数据文件读取失败",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) { Text("知道了") }
        }
    }
}
