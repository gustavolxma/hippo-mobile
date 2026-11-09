package br.com.hippo.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Paleta do app. Os nomes seguem o UI Kit do protótipo; a versão clara foi derivada
 * dele mantendo violeta e lima, com contraste mínimo de 4,5:1 nos textos (WCAG AA).
 */
@Immutable
data class HippoColorScheme(
    val background: Color,
    val surface: Color,
    /** Borda dos cartões. No escuro o próprio contraste basta, então é transparente. */
    val surfaceBorder: Color,
    /** Fundo de chips não selecionados e borda dos campos. */
    val track: Color,
    val primary: Color,
    /** Texto/ícone sobre [primary] (ex.: o "H" do logo). */
    val onPrimary: Color,
    /** Lima usado como FUNDO (botão principal, check, chip selecionado). */
    val accent: Color,
    /** Texto/ícone sobre [accent]. */
    val onAccent: Color,
    /** Lima usado como TEXTO ou ícone sobre o fundo (links, aba ativa). */
    val accentText: Color,
    val warning: Color,
    val danger: Color,
    /** Texto/ícone sobre [danger] usado como fundo. */
    val onDanger: Color,
    val onBackground: Color,
    val onMuted: Color,
    val isDark: Boolean,
)

val DarkHippoColors = HippoColorScheme(
    background = Color(0xFF17142B),
    surface = Color(0xFF241F45),
    surfaceBorder = Color.Transparent,
    track = Color(0xFF3A3366),
    primary = Color(0xFF6B46F0),
    onPrimary = Color(0xFFF4F2FF),
    accent = Color(0xFFB6F54A),
    onAccent = Color(0xFF17142B),
    accentText = Color(0xFFB6F54A),
    warning = Color(0xFFFFC53D),
    danger = Color(0xFFFF8A8A),
    onDanger = Color(0xFF17142B),
    onBackground = Color(0xFFF4F2FF),
    onMuted = Color(0xFFB5B0D6),
    isDark = true,
)

val LightHippoColors = HippoColorScheme(
    background = Color(0xFFF6F4FF),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFE4DFFA),
    track = Color(0xFFE4DFFA),
    primary = Color(0xFF6B46F0),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFB6F54A),
    onAccent = Color(0xFF17142B),
    accentText = Color(0xFF4A7A0C),
    warning = Color(0xFF9A5B00),
    danger = Color(0xFFC62F42),
    onDanger = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E1A33),
    onMuted = Color(0xFF625C80),
    isDark = false,
)

internal val LocalHippoColors = staticCompositionLocalOf { DarkHippoColors }

/** Atalho para as cores do tema ativo: `HippoColors.Primary`, `HippoColors.Surface`... */
object HippoColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.surface
    val SurfaceBorder: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.surfaceBorder
    val Track: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.track
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.primary
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.onPrimary
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.onAccent
    val AccentText: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.accentText
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.warning
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.danger
    val OnDanger: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.onDanger
    val OnBackground: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.onBackground
    val OnMuted: Color @Composable @ReadOnlyComposable get() = LocalHippoColors.current.onMuted
}

/**
 * Fundo de cartão do app: superfície + borda sutil. No tema escuro a borda é
 * transparente; no claro ela separa o cartão branco do fundo quase branco.
 */
@Composable
fun Modifier.hippoCard(shape: Shape): Modifier =
    this
        .background(HippoColors.Surface, shape)
        .border(1.dp, HippoColors.SurfaceBorder, shape)
