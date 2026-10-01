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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- 浅色回退方案（Android 12 以下）
// secondary / tertiary 特意压暗：三者亮度接近，才能让 primary→secondary→tertiary 渐变上的
// onPrimary 文字保持达标对比度（原 tertiary #D97706 上白字仅 ~2.9:1）。

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1B1B2F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B2F),
    surfaceVariant = Color(0xFFEEEDF7),
    onSurfaceVariant = Color(0xFF4B4B63),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFE),
    surfaceContainer = Color(0xFFF4F4FA),
    surfaceContainerHigh = Color(0xFFEFEFF7),
    surfaceContainerHighest = Color(0xFFE9E9F3),
    outline = Color(0xFFB9B9CC),
    outlineVariant = Color(0xFFE2E2EE),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

// ---------------------------------------------------------------- 深色回退方案

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF1E2A6E),
    primaryContainer = Color(0xFF3648A8),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF7FD8CE),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF9CF5EA),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = Color(0xFF111116),
    onBackground = Color(0xFFE6E3EB),
    surface = Color(0xFF191920),
    onSurface = Color(0xFFE6E3EB),
    surfaceVariant = Color(0xFF26262F),
    onSurfaceVariant = Color(0xFFC9C6D2),
    surfaceContainerLowest = Color(0xFF0C0C11),
    surfaceContainerLow = Color(0xFF16161D),
    surfaceContainer = Color(0xFF1B1B23),
    surfaceContainerHigh = Color(0xFF26262F),
    surfaceContainerHighest = Color(0xFF31313B),
    outline = Color(0xFF8F8F9C),
    outlineVariant = Color(0xFF3A3A46),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ---------------------------------------------------------------- 形状与字阶

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/** 统一字阶：各页面仍可显式指定 fontSize，这里提供一致的主题级兜底。 */
private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

// ---------------------------------------------------------------- 扩展色

/**
 * 主题体系之外的颜色与渐变令牌：
 * - accents：四个记忆模式的分类识别色。分类色需要长期稳定，不随系统动态取色漂移，
 *   因此固定为品牌色，深色模式换成对比度更高的浅色调。
 * - success：答题正确/导入成功等语义色（Material3 无 success 角色）。
 * - heroStops / ringStops / glowStops：渐变停靠点，由当前配色方案派生。
 *   在 Material 3 动态配色里 primary / secondary / tertiary 于浅色同为 tone 40、
 *   深色同为 tone 80，亮度天然接近，因此这三者之间的渐变不会让 onPrimary 文字失守。
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
    val ringStops: List<Color>,
    val glowStops: List<Color>
) {
    /** 按序号取分类色，超出范围时循环。 */
    fun accent(index: Int): Color = when (((index % 4) + 4) % 4) {
        0 -> accent1
        1 -> accent2
        2 -> accent3
        else -> accent4
    }
}

private val LightAccents = listOf(
    Color(0xFF4F46E5), // 靛蓝
    Color(0xFF0D9488), // 青
    Color(0xFFD97706), // 琥珀
    Color(0xFFDB2777)  // 粉
)

private val DarkAccents = listOf(
    Color(0xFF818CF8),
    Color(0xFF2DD4BF),
    Color(0xFFFBBF24),
    Color(0xFFF472B6)
)

private fun appColorsFor(scheme: ColorScheme, dark: Boolean): AppColors {
    val accents = if (dark) DarkAccents else LightAccents
    return AppColors(
        accent1 = accents[0],
        accent2 = accents[1],
        accent3 = accents[2],
        accent4 = accents[3],
        success = if (dark) Color(0xFF4ADE80) else Color(0xFF15803D),
        successContainer = if (dark) Color(0xFF14532D) else Color(0xFFDCFCE7),
        onSuccessContainer = if (dark) Color(0xFFBBF7D0) else Color(0xFF14532D),
        heroStops = listOf(scheme.primary, scheme.secondary, scheme.tertiary),
        ringStops = listOf(scheme.primary, scheme.tertiary, scheme.primary),
        glowStops = listOf(scheme.primary, scheme.secondary)
    )
}

val LocalAppColors = staticCompositionLocalOf { appColorsFor(LightColors, dark = false) }

// ---------------------------------------------------------------- 主题入口

@Composable
fun EnglishWordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dark = darkTheme
    val context = LocalContext.current
    val colorScheme = when {
        // 使用系统动态颜色（Android 12+），否则使用固定的浅色/深色方案
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val appColors = remember(colorScheme, dark) { appColorsFor(colorScheme, dark) }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}
