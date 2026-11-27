package com.fitotv.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.fitotv.R

/** Colores de FitoTV (versión 2, azul rey). */
object Fc {
    val Noche = Color(0xFF0A1747)
    val Panel = Color(0xFF0D1C55)
    val S1 = Color(0xFF0F1F5C)
    val S2 = Color(0xFF12235F)
    val S3 = Color(0xFF1A2E78)
    val Line = Color(0xFF1A2C6E)
    val Line2 = Color(0xFF22397F)
    val Line3 = Color(0xFF2A3F86)
    val Text = Color(0xFFF5F7FF)
    val Muted = Color(0xFFAEBBEB)
    val Muted2 = Color(0xFFC8D2F5)
    val Dim = Color(0xFF8E9BD0)
    val Num = Color(0xFF9DB2FF)
    val Track = Color(0xFF1F3378)
    val Azul = Color(0xFF2350E0)
    val Menta = Color(0xFF3DE0BE)
    val Ambar = Color(0xFFFFC53D)
    val MentaSoft = Color(0x293DE0BE)
    val Glow = Color(0xFF13298A)

    /** Colores para las iniciales de los canales que no traen logo. */
    val tiles = listOf(
        Color(0xFF3A55B8), Color(0xFF1F3A9E), Color(0xFF6A4FC2), Color(0xFF1F7A8C),
        Color(0xFF2E6A8F), Color(0xFF4B3FA8), Color(0xFF1F5F9E), Color(0xFF3A6FB8),
    )

    fun tileFor(key: String): Color = tiles[(key.hashCode() and 0x7fffffff) % tiles.size]
}

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
)

val Bricolage = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

/** Texto con la tipografía de FitoTV. Los tamaños son los mismos números del diseño. */
@Composable
fun T(
    text: String,
    size: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    color: Color = Fc.Text,
    weight: FontWeight = FontWeight.Normal,
    display: Boolean = false,
    maxLines: Int = 1,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = if (display && weight == FontWeight.Normal) FontWeight.ExtraBold else weight,
            fontFamily = if (display) Bricolage else Jakarta,
            textAlign = align ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
        ),
    )
}
