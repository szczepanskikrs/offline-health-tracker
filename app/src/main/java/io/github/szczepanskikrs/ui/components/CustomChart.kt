package io.github.szczepanskikrs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.szczepanskikrs.data.MeasurementEntry
import io.github.szczepanskikrs.data.MeasurementType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomChart(
    entries: List<MeasurementEntry>,
    type: MeasurementType,
    modifier: Modifier = Modifier
) {
    val sortedEntries = entries.filter { it.type == type }.sortedBy { it.timestamp }

    if (sortedEntries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Brak danych do wyświetlenia wykresu trendu.\nDodaj kilka pomiarów!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        fontSize = 10.sp
    )

    // Determine Y Bounds
    val isBP = type == MeasurementType.BLOOD_PRESSURE
    var minY: Double
    var maxY: Double

    if (isBP) {
        val allSystolic = sortedEntries.map { it.value1 }
        val allDiastolic = sortedEntries.map { it.value2 ?: 80.0 }
        minY = allDiastolic.minOrNull() ?: 60.0
        maxY = allSystolic.maxOrNull() ?: 140.0
    } else {
        val allValues = sortedEntries.map { it.value1 }
        minY = allValues.minOrNull() ?: 0.0
        maxY = allValues.maxOrNull() ?: 100.0
    }

    // Add some padding to bounds so lines don't hit the absolute edges
    val yRange = maxY - minY
    val yPadding = if (yRange == 0.0) 10.0 else yRange * 0.15
    minY = (minY - yPadding).coerceAtLeast(0.0)
    maxY += yPadding

    // Primary & Secondary colors
    val primaryColor = when (type) {
        MeasurementType.WEIGHT -> Color(0xFF6366F1) // Indigo
        MeasurementType.BLOOD_PRESSURE -> Color(0xFFEF4444) // Rose/Red for Systolic
        MeasurementType.BLOOD_SUGAR -> Color(0xFF10B981) // Emerald
    }
    val secondaryColor = Color(0xFF3B82F6) // Blue for Diastolic

    val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            // Margins for axes
            val leftMargin = 100f
            val bottomMargin = 60f
            val chartWidth = width - leftMargin - 20f
            val chartHeight = height - bottomMargin - 20f

            // Helper function to convert data point to screen coordinates
            fun getCoordinates(index: Int, value: Double): Offset {
                val x = if (sortedEntries.size > 1) {
                    leftMargin + (index.toFloat() / (sortedEntries.size - 1)) * chartWidth
                } else {
                    leftMargin + chartWidth / 2f
                }
                
                val relativeY = (value - minY) / (maxY - minY)
                val y = height - bottomMargin - (relativeY.toFloat() * chartHeight)
                return Offset(x, y)
            }

            // 1. Draw horizontal grid lines and Y-axis labels
            val gridLinesCount = 4
            for (i in 0..gridLinesCount) {
                val fraction = i.toFloat() / gridLinesCount
                val value = minY + fraction * (maxY - minY)
                val y = height - bottomMargin - (fraction * chartHeight)
                
                // Draw grid line
                if (i > 0 && i < gridLinesCount) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(leftMargin, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Y label
                val labelText = if (isBP) "${value.toInt()}" else String.format(Locale.US, "%.1f", value)
                val layoutResult = textMeasurer.measure(labelText, labelStyle)
                drawText(
                    textLayoutResult = layoutResult,
                    topLeft = Offset(leftMargin - layoutResult.size.width - 15f, y - layoutResult.size.height / 2f)
                )
            }

            // 2. Draw X-axis labels (dates)
            val maxLabels = 5
            val step = if (sortedEntries.size > maxLabels) sortedEntries.size / (maxLabels - 1) else 1
            for (i in sortedEntries.indices) {
                if (i % step == 0 || i == sortedEntries.lastIndex) {
                    val entry = sortedEntries[i]
                    val coord = getCoordinates(i, entry.value1)
                    val labelText = dateFormat.format(Date(entry.timestamp))
                    val layoutResult = textMeasurer.measure(labelText, labelStyle)
                    drawText(
                        textLayoutResult = layoutResult,
                        topLeft = Offset(coord.x - layoutResult.size.width / 2f, height - bottomMargin + 10f)
                    )
                }
            }

            // Draw axis lines
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(leftMargin, height - bottomMargin),
                end = Offset(width, height - bottomMargin),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(leftMargin, 0f),
                end = Offset(leftMargin, height - bottomMargin),
                strokeWidth = 1.5.dp.toPx()
            )

            // 3. Draw Plot Lines & Areas
            if (isBP) {
                // Draw Area between Systolic and Diastolic
                val fillPath = Path()
                if (sortedEntries.isNotEmpty()) {
                    val startCoordSys = getCoordinates(0, sortedEntries[0].value1)
                    fillPath.moveTo(startCoordSys.x, startCoordSys.y)
                    for (i in 1 until sortedEntries.size) {
                        val coord = getCoordinates(i, sortedEntries[i].value1)
                        fillPath.lineTo(coord.x, coord.y)
                    }
                    for (i in sortedEntries.indices.reversed()) {
                        val coord = getCoordinates(i, sortedEntries[i].value2 ?: 80.0)
                        fillPath.lineTo(coord.x, coord.y)
                    }
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), secondaryColor.copy(alpha = 0.2f)),
                            startY = 0f,
                            endY = height
                        )
                    )
                }

                // Draw Systolic Line (primaryColor)
                val sysPath = Path()
                val diaPath = Path()

                sortedEntries.forEachIndexed { i, entry ->
                    val sysCoord = getCoordinates(i, entry.value1)
                    val diaCoord = getCoordinates(i, entry.value2 ?: 80.0)

                    if (i == 0) {
                        sysPath.moveTo(sysCoord.x, sysCoord.y)
                        diaPath.moveTo(diaCoord.x, diaCoord.y)
                    } else {
                        sysPath.lineTo(sysCoord.x, sysCoord.y)
                        diaPath.lineTo(diaCoord.x, diaCoord.y)
                    }
                }

                drawPath(
                    path = sysPath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                drawPath(
                    path = diaPath,
                    color = secondaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Points
                sortedEntries.forEachIndexed { i, entry ->
                    val sysCoord = getCoordinates(i, entry.value1)
                    val diaCoord = getCoordinates(i, entry.value2 ?: 80.0)

                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = sysCoord)
                    drawCircle(color = primaryColor, radius = 3.dp.toPx(), style = Stroke(width = 2.dp.toPx()), center = sysCoord)

                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = diaCoord)
                    drawCircle(color = secondaryColor, radius = 3.dp.toPx(), style = Stroke(width = 2.dp.toPx()), center = diaCoord)
                }

            } else {
                // Single Line Chart with under-area gradient
                val linePath = Path()
                val fillPath = Path()

                sortedEntries.forEachIndexed { i, entry ->
                    val coord = getCoordinates(i, entry.value1)
                    if (i == 0) {
                        linePath.moveTo(coord.x, coord.y)
                        fillPath.moveTo(coord.x, height - bottomMargin)
                        fillPath.lineTo(coord.x, coord.y)
                    } else {
                        linePath.lineTo(coord.x, coord.y)
                        fillPath.lineTo(coord.x, coord.y)
                    }

                    if (i == sortedEntries.lastIndex) {
                        fillPath.lineTo(coord.x, height - bottomMargin)
                        fillPath.close()
                    }
                }

                // Draw gradient under-area
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = minY.toFloat(), // approximate
                        endY = height - bottomMargin
                    )
                )

                // Draw connection path
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw point markers
                sortedEntries.forEachIndexed { i, entry ->
                    val coord = getCoordinates(i, entry.value1)
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = coord
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx()),
                        center = coord
                    )
                }
            }
        }
    }
}
