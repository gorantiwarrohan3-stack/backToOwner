package com.wpi.backtoowner.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import kotlin.math.max

private val LostColor = Color(0xFFAC2B37)
private val FoundColor = Color(0xFF2E7D32)
private val AxisColor = Color(0xFF9E9E9E)

@Composable
fun LostFoundBarChart(lost: Int, found: Int, modifier: Modifier = Modifier) {
    val total = max(1, lost + found)
    val lostFrac = lost.toFloat() / total
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        val w = size.width
        val h = size.height
        drawRoundRect(color = LostColor.copy(alpha = 0.85f), cornerRadius = CornerRadius(8.dp.toPx()))
        drawRoundRect(
            color = FoundColor.copy(alpha = 0.9f),
            topLeft = Offset(lostFrac * w, 0f),
            size = Size(w * (1f - lostFrac), h),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Lost $lost", style = MaterialTheme.typography.labelSmall, color = LostColor)
        Text("Found $found", style = MaterialTheme.typography.labelSmall, color = FoundColor)
    }
}

@Composable
fun DailyVolumeBarChart(days: List<DayCount>, modifier: Modifier = Modifier) {
    val maxVal = max(1, days.maxOfOrNull { it.count } ?: 1)
    val barAreaHeight = 96.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "${day.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WpiHeaderMaroon,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barAreaHeight),
                ) {
                    Canvas(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(barAreaHeight),
                    ) {
                        val barH = size.height * (day.count.toFloat() / maxVal)
                        drawRoundRect(
                            color = WpiHeaderMaroon.copy(alpha = 0.88f),
                            topLeft = Offset(0f, size.height - barH),
                            size = Size(size.width, barH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                    }
                }
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = AxisColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun TopTitlesHorizontalBars(items: List<TitleCount>, modifier: Modifier = Modifier) {
    val maxVal = max(1, items.maxOfOrNull { it.count } ?: 1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { row ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF333333),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${row.count}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WpiHeaderMaroon,
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(top = 4.dp),
                ) {
                    val frac = row.count.toFloat() / maxVal
                    drawRoundRect(
                        color = Color(0xFFE0E0E0),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    drawRoundRect(
                        color = WpiHeaderMaroon.copy(alpha = 0.9f),
                        size = Size(size.width * frac, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
fun TrendLineChart(days: List<DayCount>, modifier: Modifier = Modifier) {
    val maxVal = max(1, days.maxOfOrNull { it.count } ?: 1)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp),
    ) {
        val n = days.size
        if (n < 2) return@Canvas
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        val points = days.mapIndexed { i, d ->
            val x = i * stepX
            val y = size.height - (size.height * (d.count.toFloat() / maxVal))
            Offset(x, y.coerceIn(0f, size.height))
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(path, color = WpiHeaderMaroon, style = Stroke(width = 3.dp.toPx()))
        points.forEach { p ->
            drawCircle(color = WpiHeaderMaroon, radius = 4.dp.toPx(), center = p)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = p)
        }
        drawLine(
            color = AxisColor.copy(alpha = 0.4f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}
