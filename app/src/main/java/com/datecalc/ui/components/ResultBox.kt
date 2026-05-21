package com.datecalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecalc.logic.DateCalcResult

private enum class CutoutPos { BottomRight, BottomLeft, TopRight, TopLeft }

private class ConcaveCutoutShape(
    private val cutout: CutoutPos,
    private val cutR: Dp,
    private val cr: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cr.toPx() }
        val c = with(density) { cutR.toPx() }
        val w = size.width
        val h = size.height
        val p = Path()
        when (cutout) {
            CutoutPos.BottomRight -> {
                p.moveTo(r, 0f)
                p.quadraticBezierTo(0f, 0f, 0f, r)
                p.lineTo(0f, h - r)
                p.quadraticBezierTo(0f, h, r, h)
                p.lineTo(w - c, h)
                p.quadraticBezierTo(w, h, w, h - c)
                p.lineTo(w, r)
                p.quadraticBezierTo(w, 0f, w - r, 0f)
                p.close()
            }
            CutoutPos.BottomLeft -> {
                p.moveTo(r, 0f)
                p.quadraticBezierTo(0f, 0f, 0f, r)
                p.lineTo(0f, h - c)
                p.quadraticBezierTo(0f, h, c, h)
                p.lineTo(w - r, h)
                p.quadraticBezierTo(w, h, w, h - r)
                p.lineTo(w, r)
                p.quadraticBezierTo(w, 0f, w - r, 0f)
                p.close()
            }
            CutoutPos.TopRight -> {
                p.moveTo(r, 0f)
                p.lineTo(w - c, 0f)
                p.quadraticBezierTo(w, 0f, w, c)
                p.lineTo(w, h - r)
                p.quadraticBezierTo(w, h, w - r, h)
                p.lineTo(r, h)
                p.quadraticBezierTo(0f, h, 0f, h - r)
                p.lineTo(0f, r)
                p.quadraticBezierTo(0f, 0f, r, 0f)
                p.close()
            }
            CutoutPos.TopLeft -> {
                p.moveTo(c, 0f)
                p.quadraticBezierTo(0f, 0f, 0f, c)
                p.lineTo(0f, h - r)
                p.quadraticBezierTo(0f, h, r, h)
                p.lineTo(w - r, h)
                p.quadraticBezierTo(w, h, w, h - r)
                p.lineTo(w, r)
                p.quadraticBezierTo(w, 0f, w - r, 0f)
                p.close()
            }
        }
        return Outline.Generic(p)
    }
}

@Composable
fun ResultBox(
    result: DateCalcResult,
    description: String,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val resultBg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF1F2938)
    val resultOnBg = if (isLight) Color(0xFF1C1C1E) else Color.White
    val resultDescColor = if (isLight) Color(0xFF3C3C43) else Color.White.copy(alpha = 0.75f)
    val topRowBg = if (isLight) Color.White else Color.White.copy(alpha = 0.10f)
    val bottomRowBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF384561)
    val tileTitleColor = if (isLight) Color(0xFF8E8E93) else Color.White.copy(alpha = 0.72f)
    val tileValueColor = if (isLight) Color(0xFF1C1C1E) else Color.White
    val tileStroke = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)
    val accentColor = Color(0xFF007AFF)
    val cutoutR = 20.dp
    val circleSize = 36.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = resultBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Результат", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = resultOnBg)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = resultDescColor)
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                        CutoutTile(
                            "Дней", "${result.days}", topRowBg, tileTitleColor, tileValueColor, tileStroke,
                            CutoutPos.BottomRight, cutoutR, Modifier.weight(1f)
                        )
                        CutoutTile(
                            "Недель", "${result.weeks}", topRowBg, tileTitleColor, tileValueColor, tileStroke,
                            CutoutPos.BottomLeft, cutoutR, Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                        CutoutTile(
                            "Месяцев", "${result.months}", bottomRowBg, tileTitleColor, tileValueColor, tileStroke,
                            CutoutPos.TopRight, cutoutR, Modifier.weight(1f)
                        )
                        CutoutTile(
                            "Остаток дней", "${result.remainingDays}", bottomRowBg, tileTitleColor, tileValueColor, tileStroke,
                            CutoutPos.TopLeft, cutoutR, Modifier.weight(1f)
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    val circleBg = if (isLight) Color.White else Color(0xFF2C3A50)
                    Surface(
                        shape = CircleShape,
                        color = circleBg,
                        modifier = Modifier
                            .size(circleSize)
                            .border(2.dp, accentColor, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${result.years}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CutoutTile(
    title: String,
    value: String,
    background: Color,
    titleColor: Color,
    valueColor: Color,
    strokeColor: Color,
    cutoutPos: CutoutPos,
    cutoutRadius: Dp,
    modifier: Modifier = Modifier
) {
    val shape = ConcaveCutoutShape(cutoutPos, cutoutRadius)
    val innerPad = 10.dp
    val density = LocalDensity.current
    val cutoutPad = with(density) { (cutoutRadius.toPx() * 0.35f).toDp() }
    val contentModifier = when (cutoutPos) {
        CutoutPos.BottomRight -> Modifier.padding(start = innerPad, top = innerPad, end = innerPad + cutoutPad, bottom = innerPad + cutoutPad)
        CutoutPos.BottomLeft -> Modifier.padding(start = innerPad + cutoutPad, top = innerPad, end = innerPad, bottom = innerPad + cutoutPad)
        CutoutPos.TopRight -> Modifier.padding(start = innerPad, top = innerPad + cutoutPad, end = innerPad + cutoutPad, bottom = innerPad)
        CutoutPos.TopLeft -> Modifier.padding(start = innerPad + cutoutPad, top = innerPad + cutoutPad, end = innerPad, bottom = innerPad)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, strokeColor, shape)
            .then(contentModifier)
    ) {
        Column {
            Text(title, fontSize = 11.sp, color = titleColor)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
