package com.jr.englishword.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 圆角色块 + 主题色图标，用作卡片内的图标位。 */
@Composable
fun IconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 14.dp,
    containerAlpha: Float = 0.12f
) {
    Box(
        modifier
            .size(size)
            .background(tint.copy(alpha = containerAlpha), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 渐变版图标块：比纯色块更有层次，用于首页卡片与设置页卡头。 */
@Composable
fun GradientIconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 23.dp,
    cornerRadius: Dp = 15.dp
) {
    Box(
        modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.26f), tint.copy(alpha = 0.09f))
                ),
                RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 计数 / 状态小标签。 */
@Composable
fun InfoPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = contentColor
        )
    }
}

/** 卡片标题行：图标块 + 标题（+ 可选的尾部内容）。 */
@Composable
fun SectionCardHeader(
    icon: ImageVector,
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        GradientIconChip(icon = icon, tint = tint, size = 36.dp, iconSize = 19.dp, cornerRadius = 12.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
