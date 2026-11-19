package com.jr.englishword.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 书卷衬线字体族：用于英文词头、词性标记及核心统计数字。 */
val EditorialSerif: FontFamily = FontFamily.Serif

// ---------------------------------------------------------------- 温润书卷浅色方案

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B4C59),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3ECEF),
    onPrimaryContainer = Color(0xFF162B33),
    secondary = Color(0xFF3E6B58),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EFE9),
    onSecondaryContainer = Color(0xFF163327),
    tertiary = Color(0xFF8C5A32),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5EAE0),
    onTertiaryContainer = Color(0xFF3D230E),
    background = Color(0xFFFBF9F5),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFBF9F5),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEEEAE2),
    onSurfaceVariant = Color(0xFF666259),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F1EA),
    surfaceContainer = Color(0xFFEEEAE2),
    surfaceContainerHigh = Color(0xFFE6E1D7),
    surfaceContainerHighest = Color(0xFFDED8CC),
    outline = Color(0xFF9C968B),
    outlineVariant = Color(0xFFE6E1D6),
    inverseSurface = Color(0xFF262521),
    inverseOnSurface = Color(0xFFF6F3EC),
    inversePrimary = Color(0xFF9AC2D1),
    error = Color(0xFFB83B3B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBEAEA),
    onErrorContainer = Color(0xFF5C1919)
)

// ---------------------------------------------------------------- 深邃墨色深色方案

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AC2D1),
    onPrimary = Color(0xFF102830),
    primaryContainer = Color(0xFF213D48),
    onPrimaryContainer = Color(0xFFD6EAF2),
    secondary = Color(0xFF96C4B0),
    onSecondary = Color(0xFF122E22),
    secondaryContainer = Color(0xFF224235),
    onSecondaryContainer = Color(0xFFD6EFE3),
    tertiary = Color(0xFFDFB18B),
    onTertiary = Color(0xFF3B210E),
    tertiaryContainer = Color(0xFF54341C),
    onTertiaryContainer = Color(0xFFF8E2D0),
    background = Color(0xFF131416),
    onBackground = Color(0xFFECE9E2),
    surface = Color(0xFF131416),
    onSurface = Color(0xFFECE9E2),
    surfaceVariant = Color(0xFF24262B),
    onSurfaceVariant = Color(0xFF9E9B93),
    surfaceContainerLowest = Color(0xFF1B1C20),
    surfaceContainerLow = Color(0xFF17181C),
    surfaceContainer = Color(0xFF202227),
    surfaceContainerHigh = Color(0xFF282A30),
    surfaceContainerHighest = Color(0xFF32353C),
    outline = Color(0xFF6E6B64),
    outlineVariant = Color(0xFF2B2D33),
    inverseSurface = Color(0xFFECE9E2),
    inverseOnSurface = Color(0xFF1B1C20),
    inversePrimary = Color(0xFF2B4C59),
    error = Color(0xFFECA1A1),
    onError = Color(0xFF4F1212),
    errorContainer = Color(0xFF471E1E),
    onErrorContainer = Color(0xFFFADADA)
)

// ---------------------------------------------------------------- 形状与字阶

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/** 统一字阶：兼顾中文阅读呼吸感与层级清晰度。 */
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

// ---------------------------------------------------------------- 扩展色令牌

/**
 * 书卷极简扩展色令牌：
 * - accent1..4：四种记忆模式的低饱和度雅致识别色（黛青、松石绿、赭石、暮紫）。
 * - success：答题正确与导入成功的柔和竹青语义色。
 * - ringStops：进度环与进度条的双色墨韵过渡。
 */
@Immutable
data class AppColors(
    val accent1: Color,
    val accent2: Color,
    val accent3: Color,
    val accent4: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val heroStops: List<Color>,
    val ringStops: List<Color>
) {
    /** 按序号循环取分类点缀色。 */
    fun accent(index: Int): Color = when (((index % 4) + 4) % 4) {
        0 -> accent1
        1 -> accent2
        2 -> accent3
        else -> accent4
    }
}

private val LightAccents = listOf(
    Color(0xFF2B4C59), // 黛青（英译中）
    Color(0xFF3E6B58), // 松绿（中译英）
    Color(0xFF8C5A32), // 赭石（默写中文）
    Color(0xFF6B4E71)  // 暮紫（拼写英文）
)

private val DarkAccents = listOf(
    Color(0xFF9AC2D1),
    Color(0xFF96C4B0),
    Color(0xFFDFB18B),
    Color(0xFFC5A6CC)
)

private fun appColorsFor(scheme: ColorScheme, dark: Boolean): AppColors {
    val accents = if (dark) DarkAccents else LightAccents
    return AppColors(
        accent1 = accents[0],
        accent2 = accents[1],
        accent3 = accents[2],
        accent4 = accents[3],
        success = if (dark) Color(0xFF7CC9A2) else Color(0xFF2A6F4E),
        successContainer = if (dark) Color(0xFF193829) else Color(0xFFE5F3EC),
        onSuccessContainer = if (dark) Color(0xFFC8EEDB) else Color(0xFF153D29),
        heroStops = listOf(scheme.primary, scheme.secondary),
        ringStops = listOf(scheme.primary, scheme.secondary, scheme.primary)
    )
}

val LocalAppColors = staticCompositionLocalOf { appColorsFor(LightColors, dark = false) }

// ---------------------------------------------------------------- 主题入口

@Composable
fun EnglishWordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val appColors = remember(colorScheme, darkTheme) { appColorsFor(colorScheme, darkTheme) }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}
