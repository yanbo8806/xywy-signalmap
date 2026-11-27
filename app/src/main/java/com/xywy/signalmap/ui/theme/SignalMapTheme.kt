package com.xywy.signalmap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================
//  设计令牌 · 品牌色
//  主色「信号蓝」：可信、专业的网络运维蓝
//  辅色「频谱青」：健康、在线状态
//  第三色「警戒橙」：新建、工具、强调动作
// ============================================================

private val Primary = Color(0xFF1D68EE)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFE8F1FF)
private val OnPrimaryContainer = Color(0xFF0C3278)

private val Secondary = Color(0xFF059669)
private val OnSecondary = Color(0xFFFFFFFF)
private val SecondaryContainer = Color(0xFFD1FAE5)
private val OnSecondaryContainer = Color(0xFF064E3B)

private val Tertiary = Color(0xFFF59E0B)
private val OnTertiary = Color(0xFF451A03)
private val TertiaryContainer = Color(0xFFFEF3C7)
private val OnTertiaryContainer = Color(0xFF78350F)

private val Error = Color(0xFFB91C1C)
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFFFEE2E2)
private val OnErrorContainer = Color(0xFF7F1D1D)

private val Background = Color(0xFFF8FAFC)
private val OnBackground = Color(0xFF0F172A)
private val Surface = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFF0F172A)
private val SurfaceVariant = Color(0xFFF1F5F9)
private val OnSurfaceVariant = Color(0xFF64748B)
private val Outline = Color(0xFF94A3B8)
private val OutlineVariant = Color(0xFFE2E8F0)
private val InverseSurface = Color(0xFF1E293B)
private val InverseOnSurface = Color(0xFFF8FAFC)
private val InversePrimary = Color(0xFF93C5FD)
private val Scrim = Color(0xFF000000)

private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF8FAFC)
private val SurfaceContainer = Color(0xFFF1F5F9)
private val SurfaceContainerHigh = Color(0xFFE8EEF5)
private val SurfaceContainerHighest = Color(0xFFDEE5EF)
private val SurfaceDim = Color(0xFFE2E8F0)
private val SurfaceBright = Color(0xFFFFFFFF)

// Dark Theme —— 深空海军蓝，层次由 surfaceContainer 阶梯表达
private val DarkPrimary = Color(0xFF6BA6FF)
private val DarkOnPrimary = Color(0xFF0A2E6B)
private val DarkPrimaryContainer = Color(0xFF103E8A)
private val DarkOnPrimaryContainer = Color(0xFFE0EDFF)

private val DarkSecondary = Color(0xFF34D399)
private val DarkOnSecondary = Color(0xFF023C27)
private val DarkSecondaryContainer = Color(0xFF065F46)
private val DarkOnSecondaryContainer = Color(0xFFD1FAE5)

private val DarkTertiary = Color(0xFFFBBF24)
private val DarkOnTertiary = Color(0xFF451A03)
private val DarkTertiaryContainer = Color(0xFF78350F)
private val DarkOnTertiaryContainer = Color(0xFFFEF3C7)

private val DarkError = Color(0xFFF87171)
private val DarkOnError = Color(0xFF450A0A)
private val DarkErrorContainer = Color(0xFF7F1D1D)
private val DarkOnErrorContainer = Color(0xFFFEE2E2)

private val DarkBackground = Color(0xFF0A0E17)
private val DarkOnBackground = Color(0xFFF1F5F9)
private val DarkSurface = Color(0xFF111827)
private val DarkOnSurface = Color(0xFFF1F5F9)
private val DarkSurfaceVariant = Color(0xFF1E293B)
private val DarkOnSurfaceVariant = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF475569)
private val DarkOutlineVariant = Color(0xFF1E293B)
private val DarkInverseSurface = Color(0xFFF1F5F9)
private val DarkInverseOnSurface = Color(0xFF0F172A)
private val DarkInversePrimary = Color(0xFF1D68EE)
private val DarkScrim = Color(0xFF000000)

private val DarkSurfaceContainerLowest = Color(0xFF070B12)
private val DarkSurfaceContainerLow = Color(0xFF0D131F)
private val DarkSurfaceContainer = Color(0xFF121926)
private val DarkSurfaceContainerHigh = Color(0xFF182234)
private val DarkSurfaceContainerHighest = Color(0xFF1F2C42)
private val DarkSurfaceDim = Color(0xFF0A0E17)
private val DarkSurfaceBright = Color(0xFF2A364C)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    scrim = Scrim,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    scrim = DarkScrim,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

// ============================================================
//  字体系统：标题收紧、层级分明，正文保持舒适行高
// ============================================================
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.25.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    ),
)

// ============================================================
//  形状系统：圆润高质感
// ============================================================
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ============================================================
//  扩展语义色：成功/警告/信息、毛玻璃、渐变等品牌扩展令牌
// ============================================================
@Immutable
data class XywyColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val cardBorder: Color,
    val glass: Color,
    val glassBorder: Color,
    val glassShadow: Color,
    val heroGradient: List<Color>,
    val heroText: Color,
    val heroSubText: Color,
) {
    val heroBrush: Brush
        get() = Brush.linearGradient(heroGradient)
}

private fun lightXywyColors() = XywyColors(
    success = Color(0xFF047857),
    successContainer = Color(0xFFD1FAE5),
    onSuccessContainer = Color(0xFF065F46),
    warning = Color(0xFF92400E),
    warningContainer = Color(0xFFFEF3C7),
    info = Color(0xFF0284C7),
    infoContainer = Color(0xFFE0F2FE),
    cardBorder = Color(0xFFE2E8F0),
    glass = Color(0xF2FFFFFF),
    glassBorder = Color(0x2E0F172A),
    glassShadow = Color(0x180F172A),
    heroGradient = listOf(Color(0xFF1652C7), Color(0xFF1D4ED8), Color(0xFF0F766E)),
    heroText = Color(0xFFFFFFFF),
    heroSubText = Color(0xFFDBEAFE),
)

private fun darkXywyColors() = XywyColors(
    success = Color(0xFF34D399),
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFD1FAE5),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF78350F),
    info = Color(0xFF38BDF8),
    infoContainer = Color(0xFF0C4A6E),
    cardBorder = Color(0xFF1E293B),
    glass = Color(0xE80D131F),
    glassBorder = Color(0x3394A3B8),
    glassShadow = Color(0x55000000),
    heroGradient = listOf(Color(0xFF0F3278), Color(0xFF1D4ED8), Color(0xFF0F766E)),
    heroText = Color(0xFFF8FAFC),
    heroSubText = Color(0xFFDBEAFE),
)

val LocalXywyColors = staticCompositionLocalOf { lightXywyColors() }

val MaterialTheme.xywy: XywyColors
    @Composable get() = LocalXywyColors.current

// ============================================================
//  信号语义色：强度五档 + 制式配色（明暗主题下均保持可读）
// ============================================================
object SignalColors {
    val Excellent = Color(0xFF10B981)
    val Good = Color(0xFF84CC16)
    val Fair = Color(0xFFF59E0B)
    val Poor = Color(0xFFF97316)
    val Bad = Color(0xFFEF4444)

    val NR = Color(0xFF2563EB)
    val LTE = Color(0xFF0D9488)
    val WCDMA = Color(0xFF8B5CF6)
    val GSM = Color(0xFFF59E0B)
    val CDMA = Color(0xFFEC4899)
    val Unknown = Color(0xFF94A3B8)

    fun forTechnology(tech: com.xywy.signalmap.domain.model.RadioTechnology): Color = when (tech) {
        com.xywy.signalmap.domain.model.RadioTechnology.NR -> NR
        com.xywy.signalmap.domain.model.RadioTechnology.LTE -> LTE
        com.xywy.signalmap.domain.model.RadioTechnology.WCDMA -> WCDMA
        com.xywy.signalmap.domain.model.RadioTechnology.GSM -> GSM
        com.xywy.signalmap.domain.model.RadioTechnology.CDMA -> CDMA
        com.xywy.signalmap.domain.model.RadioTechnology.UNKNOWN -> Unknown
    }
}

@Composable
fun SignalMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val xywyColors = remember(darkTheme) { if (darkTheme) darkXywyColors() else lightXywyColors() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            applySystemBarColors(
                window = window,
                view = view,
                darkTheme = darkTheme,
            )
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalXywyColors provides xywyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

@Suppress("DEPRECATION")
private fun applySystemBarColors(
    window: android.view.Window,
    view: android.view.View,
    darkTheme: Boolean,
) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.Transparent.toArgb()
    window.navigationBarColor = Color.Transparent.toArgb()
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
}
