package com.jr.englishword.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Indigo = Color(0xFF4F46E5)
private val IndigoContainer = Color(0xFFE0E7FF)
private val OnIndigoContainer = Color(0xFF1E1B4B)
private val Teal = Color(0xFF0D9488)
private val TealContainer = Color(0xFFCCFBF1)
private val OnTealContainer = Color(0xFF134E4A)
private val AmberContainer = Color(0xFFFEF3C7)
private val OnAmberContainer = Color(0xFF78350F)
private val Bg = Color(0xFFF6F6FB)
private val OnBg = Color(0xFF1B1B2F)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceVariant = Color(0xFFEEEDF7)
private val OnSurfaceVariant = Color(0xFF4B4B63)
private val OutlineVariant = Color(0xFFDEDEEA)
private val ErrorRed = Color(0xFFDC2626)
private val ErrorContainer = Color(0xFFFEE2E2)
private val OnErrorContainer = Color(0xFF7F1D1D)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = OnIndigoContainer,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = Bg,
    onBackground = OnBg,
    surface = Surface,
    onSurface = OnBg,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFB9B9CC),
    outlineVariant = OutlineVariant,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun EnglishWordsTheme(content: @Composable () -> Unit) {
    // 使用系统动态颜色（Android 12+），否则使用默认浅色主题
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        LightColors
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
