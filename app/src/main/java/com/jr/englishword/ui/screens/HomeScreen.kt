package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.QuizMode
import com.jr.englishword.net.DeepSeekApi
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.components.AnimatedCounter
import com.jr.englishword.ui.components.GradientHero
import com.jr.englishword.ui.components.GradientIconChip
import com.jr.englishword.ui.components.IconChip
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.components.staggeredAppear
import com.jr.englishword.ui.theme.LocalAppColors

private fun modeIcon(mode: QuizMode): ImageVector = when (mode) {
    QuizMode.EN_CN -> Icons.Rounded.Translate
    QuizMode.CN_EN -> Icons.Rounded.Spellcheck
    QuizMode.DICTATION -> Icons.Rounded.EditNote
    QuizMode.SPELLING -> Icons.Rounded.Keyboard
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
    val isAiConfigured = DeepSeekApi.isReady(settings)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        loadError?.let { msg ->
            LoadErrorBanner(msg, index = 0) { vm.clearLoadError() }
            Spacer(Modifier.height(12.dp))
        }
        HeroHeader(total, mastered, isAiConfigured)
        Spacer(Modifier.height(26.dp))

        Text(
            "选择记忆模式",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.staggeredAppear(0)
        )
        Spacer(Modifier.height(12.dp))

        if (total == 0) {
            EmptyBankCard(goImport)
            Spacer(Modifier.height(20.dp))
        } else {
            val modes = QuizMode.entries
            var index = 1
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
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("导入单词", Icons.Rounded.UploadFile, appColors.accent(0), 7, Modifier.weight(1f), goImport)
            QuickAction("单词本", Icons.Rounded.MenuBook, appColors.accent(1), 8, Modifier.weight(1f), goList)
            QuickAction("设置", Icons.Rounded.Settings, appColors.accent(2), 9, Modifier.weight(1f), goSettings)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Powered By JR Wang",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** 首页头部：主色渐变大卡 + 角落柔光，三格统计用半透明细线分隔。 */
@Composable
private fun HeroHeader(total: Int, mastered: Int, aiEnabled: Boolean) {
    val appColors = LocalAppColors.current
    GradientHero(
        stops = appColors.heroStops,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(0)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroStatNumber("词库单词", total, Modifier.weight(1f))
            HeroDivider()
            HeroStatNumber("已掌握", mastered, Modifier.weight(1f))
            HeroDivider()
            HeroStatText("AI 状态", if (aiEnabled) "开启" else "关闭", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStatNumber(label: String, value: Int, modifier: Modifier = Modifier) {
    // 不写死高度：系统字号放大时由内容撑开，避免标签被裁切
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedCounter(
            value = value,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HeroStatText(label: String, value: String, modifier: Modifier = Modifier) {
    // 同上：34sp 行高 + 4dp 间隔 + 标签已逼近原 56dp 上限，字号稍大即溢出
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(38.dp)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f))
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
            .pressableScale(source)
            .shadow(
                elevation = 14.dp,
                shape = MaterialTheme.shapes.large,
                clip = false,
                ambientColor = accent.copy(alpha = 0.42f),
                spotColor = accent.copy(alpha = 0.42f)
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 最小高度必须写在卡片内容这一层：Card 内部会再包一层 Column，
        // 那层会把子节点的 minHeight 归零，fillMaxSize 于是撑不满卡片、Arrangement.Center 失效
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp)
                .padding(17.dp),
            verticalArrangement = Arrangement.Center
        ) {
            GradientIconChip(
                icon = modeIcon(mode),
                tint = accent,
                size = 46.dp,
                iconSize = 24.dp,
                cornerRadius = 16.dp
            )
            Spacer(Modifier.height(13.dp))
            Text(
                mode.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WrongBookBanner(count: Int, index: Int, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val accent = LocalAppColors.current.accent(3)
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index)
            .pressableScale(source),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) accent.copy(alpha = 0.07f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            1.dp,
            if (count > 0) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant
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
                icon = Icons.Rounded.Assignment,
                tint = if (count > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 42.dp,
                iconSize = 22.dp,
                cornerRadius = 14.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "错题本",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (count > 0) "$count 个单词答错过，快来巩固" else "暂无错题，继续保持！",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (count > 0) {
                InfoPill(
                    text = "去重练",
                    containerColor = accent.copy(alpha = 0.16f),
                    contentColor = accent
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
                .padding(vertical = 30.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientIconChip(
                icon = Icons.Rounded.UploadFile,
                tint = primary,
                size = 60.dp,
                iconSize = 30.dp,
                cornerRadius = 20.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "词库还是空的",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "点击这里导入单词，开始学习吧",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
        // 三列卡片很窄（320dp 屏上仅约 85dp），圆角用 medium 而非 large，
        // 让图标块离卡片边缘更远，避免小屏上出现视觉裁切
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 同 ModeCard：最小高度写在内容层，居中才会真正生效
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GradientIconChip(
                icon = icon,
                tint = color,
                size = 34.dp,
                iconSize = 18.dp,
                cornerRadius = 12.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** 数据文件损坏时的常驻提示：明确告知按空数据启动、原文件已备份，而不是静默变空词库。 */
@Composable
private fun LoadErrorBanner(message: String, index: Int, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredAppear(index),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
                    size = 34.dp,
                    iconSize = 18.dp,
                    cornerRadius = 11.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "数据文件读取失败",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                message,
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
