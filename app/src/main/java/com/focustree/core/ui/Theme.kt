@file:Suppress("MagicNumber", "MatchingDeclarationName")

package com.focustree.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

object FocusTreeTokens {
    val leaf = Color(0xFF2F6B4F)
    val leafLight = Color(0xFF9BD3AF)
    val paper = Color(0xFFF8FBF6)
    val forest = Color(0xFF101512)
    val soil = Color(0xFF8A6247)
    val amber = Color(0xFF805600)
    val outlineLight = Color(0xFF52634F)
    val outlineDark = Color(0xFFB8CCB7)
    val spacingBase = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingExtraLarge = 32.dp
    val cardRadius = 8.dp
    val pixelBorder = 2.dp
    val cardShape = RoundedCornerShape(cardRadius)
    val typography = Typography()
    val shapes = Shapes(small = cardShape, medium = cardShape, large = cardShape)
}

private val LightColors =
    lightColorScheme(
        primary = FocusTreeTokens.leaf,
        onPrimary = Color.White,
        secondary = FocusTreeTokens.outlineLight,
        background = FocusTreeTokens.paper,
        surface = FocusTreeTokens.paper,
        onBackground = Color(0xFF17221A),
        onSurface = Color(0xFF17221A),
        error = Color(0xFFBA1A1A),
    )

private val DarkColors =
    darkColorScheme(
        primary = FocusTreeTokens.leafLight,
        onPrimary = Color(0xFF123522),
        secondary = FocusTreeTokens.outlineDark,
        background = FocusTreeTokens.forest,
        surface = FocusTreeTokens.forest,
        onBackground = Color(0xFFE1E9E1),
        onSurface = Color(0xFFE1E9E1),
        error = Color(0xFFFFB4AB),
    )

@Composable
fun FocusTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FocusTreeTokens.typography,
        shapes = FocusTreeTokens.shapes,
        content = content,
    )
}

/** Original block garden flourish; decorative, with no misleading screen-reader announcement. */
@Composable
fun PixelGardenMotif(modifier: Modifier = Modifier) {
    val leafColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(72.dp)
                .semantics { hideFromAccessibility() },
    ) {
        val unit = 8.dp.toPx()
        val centerX = size.width / 2f
        val baseY = size.height - unit
        val stem = leafColor
        drawRect(
            FocusTreeTokens.soil,
            androidx.compose.ui.geometry
                .Offset(centerX - unit * 3, baseY),
            androidx.compose.ui.geometry
                .Size(unit * 6, unit),
        )
        drawRect(
            stem,
            androidx.compose.ui.geometry
                .Offset(centerX - unit / 2, baseY - unit * 5),
            androidx.compose.ui.geometry.Size(
                unit,
                unit * 5,
            ),
        )
        drawRect(
            stem,
            androidx.compose.ui.geometry
                .Offset(centerX - unit * 2, baseY - unit * 4),
            androidx.compose.ui.geometry
                .Size(unit * 1.5f, unit),
        )
        drawRect(
            stem,
            androidx.compose.ui.geometry
                .Offset(centerX + unit / 2, baseY - unit * 3),
            androidx.compose.ui.geometry
                .Size(unit * 1.5f, unit),
        )
        drawRect(
            Color(0xFFF2C14E),
            androidx.compose.ui.geometry
                .Offset(centerX - unit, baseY - unit * 7),
            androidx.compose.ui.geometry
                .Size(unit * 2, unit * 2),
        )
        drawRect(
            FocusTreeTokens.outlineLight,
            androidx.compose.ui.geometry
                .Offset(centerX - unit * 3, baseY - unit),
            androidx.compose.ui.geometry
                .Size(unit * 6, unit),
            style = Stroke(width = FocusTreeTokens.pixelBorder.toPx()),
        )
    }
}
