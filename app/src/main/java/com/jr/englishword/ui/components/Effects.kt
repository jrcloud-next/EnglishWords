package com.jr.englishword.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** 统一的动效曲线。 */
fun <T> motionSpec(durationMillis: Int = 300, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    tween(durationMillis, delayMillis = delayMillis, easing = FastOutSlowInEasing)

// ---------------------------------------------------------------- 渐变容器

/**
 * 渐变底卡：linearGradient(stops) + 两枚角落柔光。
 * 光晕用 onPrimary 低透明度绘制，浅色（深彩底）与深色（浅彩底）下都能读作"光源"。
 */
@Composable
fun GradientHero(
    stops: List<Color>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    glowColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(stops))
    ) {
        // 光晕放在 matchParentSize 容器里，只做装饰、不参与父级尺寸测量
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(190.dp)
                    .offset(x = 78.dp, y = (-86).dp)
                    .background(glowColor.copy(alpha = 0.10f), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(132.dp)
                    .offset(x = (-46).dp, y = 58.dp)
                    .background(glowColor.copy(alpha = 0.08f), CircleShape)
            )
        }
        content()
    }
}

// ---------------------------------------------------------------- 进度

/** 圆形渐变进度环。 */
@Composable
fun CircularRingProgress(
    progress: Float,
    stops: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 13.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = motionSpec(durationMillis = 900),
        label = "ring"
    )
    val shown = animated

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (shown > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(stops),
                    startAngle = -90f,
                    sweepAngle = 360f * shown,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** 自绘渐变进度条（M3 的 LinearProgressIndicator 不支持渐变笔刷）。 */
@Composable
fun GradientBar(
    progress: Float,
    stops: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = motionSpec(durationMillis = 420),
        label = "bar"
    )
    val shown = animated

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(stops))
        )
    }
}

// ---------------------------------------------------------------- 数字滚动

@Composable
fun AnimatedCounter(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = motionSpec(durationMillis = 700),
        label = "counter"
    )
    Text(
        text = animated.toString(),
        style = style,
        color = color,
        modifier = modifier
    )
}

// ---------------------------------------------------------------- 按压与入场

/** 按压回弹缩放系数，配合 [MutableInteractionSource] 使用。 */
@Composable
fun pressScale(interactionSource: InteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "press"
    )
    return scale
}

/** 卡片的按压缩放 Modifier（内部自建 InteractionSource）。 */
@Composable
fun Modifier.pressableScale(source: MutableInteractionSource): Modifier {
    val scale = pressScale(source)
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 一次性错落入场：淡入 + 自下而上 22dp。
 * 非循环动画，只在首次组合时播放一次。
 */
@Composable
fun Modifier.staggeredAppear(index: Int, stepMillis: Int = 45): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceIn(0, 10) * stepMillis).toLong())
        visible = true
    }
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "appear"
    )
    val rise by animateFloatAsState(
        targetValue = if (visible) 0f else 22f,
        animationSpec = tween(durationMillis = 320),
        label = "rise"
    )
    return this.graphicsLayer {
        alpha = fade
        translationY = rise
    }
}

// ---------------------------------------------------------------- 分段控件

/** 圆角胶囊分段控件，用于替代 TabRow（行为等价，观感更精致）。 */
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = container
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val bg by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = motionSpec(durationMillis = 220),
                    label = "segBg"
                )
                val fg by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = motionSpec(durationMillis = 220),
                    label = "segFg"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onSelect(index) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = fg,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
