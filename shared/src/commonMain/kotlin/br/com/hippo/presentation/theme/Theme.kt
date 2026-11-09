package br.com.hippo.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.nunito_black
import hippo.shared.generated.resources.nunito_extrabold
import hippo.shared.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

/** Raios do UI Kit: 12 · 14 · 20 · 24 · círculo. */
object HippoShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(20.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val Circle = RoundedCornerShape(50)
}

/** Escala tipográfica do UI Kit (Nunito). Acesse com `HippoTheme.type.title` etc. */
@Immutable
data class HippoTypography(
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
)

private fun hippoTypography(family: FontFamily) = HippoTypography(
    title = TextStyle(fontFamily = family, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp),
    subtitle = TextStyle(fontFamily = family, fontSize = 18.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp),
    body = TextStyle(fontFamily = family, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    caption = TextStyle(fontFamily = family, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp),
    label = TextStyle(fontFamily = family, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 14.sp),
)

private val LocalHippoTypography = staticCompositionLocalOf<HippoTypography> {
    error("HippoTypography não fornecida: envolva a tela em HippoTheme { }")
}

object HippoTheme {
    val type: HippoTypography
        @Composable get() = LocalHippoTypography.current
}

@Composable
private fun nunitoFamily() = FontFamily(
    Font(Res.font.nunito_semibold, FontWeight.SemiBold),
    Font(Res.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(Res.font.nunito_black, FontWeight.Black),
)

/**
 * Cores do Material 3 derivadas da paleta do Hippo. Os componentes prontos do Material
 * (seletores de data e hora, menus, snackbar) usam estas cores.
 */
private fun HippoColorScheme.toMaterial() = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = primary,
    onPrimary = onPrimary,
    secondary = accent,
    onSecondary = onAccent,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onBackground,
    surfaceVariant = track,
    onSurfaceVariant = onMuted,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = track,
    error = danger,
    onError = onDanger,
    outline = track,
)

/** Tema do app. [darkTheme] vem da preferência do usuário (Claro / Escuro / Sistema). */
@Composable
fun HippoTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val family = nunitoFamily()
    val type = remember(family) { hippoTypography(family) }
    val palette = if (darkTheme) DarkHippoColors else LightHippoColors
    val colors = remember(palette) { palette.toMaterial() }
    val base = Typography()
    val typography = Typography(
        displaySmall = type.title,
        headlineSmall = type.title,
        titleLarge = type.subtitle,
        titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        bodyLarge = type.body,
        bodyMedium = type.body,
        bodySmall = type.caption,
        labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        labelMedium = type.caption,
        labelSmall = type.label,
    )

    CompositionLocalProvider(
        LocalHippoTypography provides type,
        LocalHippoColors provides palette,
        LocalContentColor provides palette.onBackground,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = Shapes(
                small = HippoShapes.Small,
                medium = HippoShapes.Medium,
                large = HippoShapes.Large,
                extraLarge = HippoShapes.ExtraLarge,
            ),
            content = content,
        )
    }
}
