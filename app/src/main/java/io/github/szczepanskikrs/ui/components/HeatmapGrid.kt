package io.github.szczepanskikrs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.szczepanskikrs.data.ExerciseLog
import io.github.szczepanskikrs.data.MeasurementEntry
import io.github.szczepanskikrs.data.MeasurementType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HeatmapGrid(
    measurements: List<MeasurementEntry>,
    exerciseLogs: List<ExerciseLog>,
    modifier: Modifier = Modifier
) {
    val numWeeks = 18 // Displays 18 weeks horizontally
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("pl")) }
    val dayOfWeekLabelFormat = remember { SimpleDateFormat("EE", Locale.forLanguageTag("pl")) }

    // Group items by date string
    val measurementsGrouped = remember(measurements) {
        measurements.groupBy { sdf.format(Date(it.timestamp)) }
    }
    val exercisesGrouped = remember(exerciseLogs) {
        exerciseLogs.groupBy { sdf.format(Date(it.timestamp)) }
    }

    // Generate grid: 18 weeks, Monday to Sunday
    val weeksList = remember {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Adjust to Monday of the current week
        val daysToSubtract = (currentDayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        
        // Go back numWeeks weeks
        calendar.add(Calendar.WEEK_OF_YEAR, -numWeeks + 1)
        
        val list = mutableListOf<List<Date>>()
        for (w in 0 until numWeeks) {
            val weekDays = mutableListOf<Date>()
            for (d in 0 until 7) {
                val dayCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.WEEK_OF_YEAR, w)
                    add(Calendar.DAY_OF_YEAR, d)
                }
                weekDays.add(dayCal.time)
            }
            list.add(weekDays)
        }
        list
    }

    var selectedDate by remember { mutableStateOf<Date?>(null) }
    val scrollState = rememberScrollState()

    // Auto-scroll to end (most recent days) on load
    LaunchedEffect(key1 = weeksList) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Aktywność (ostatnie 18 tygodni)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day names labels on the left of the grid (Mon, Wed, Fri) - NOT SCROLLABLE
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(180.dp), // aligned to cells size
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("Pn", "", "Śr", "", "Pt", "", "Nd")
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.height(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Grid of cells - SCROLLABLE
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weeksList.forEach { week ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(180.dp)
                        ) {
                            week.forEach { date ->
                                val dateStr = sdf.format(date)
                                val dayMeasurements = measurementsGrouped[dateStr] ?: emptyList()
                                val dayExercises = exercisesGrouped[dateStr] ?: emptyList()
                                val totalActivity = dayMeasurements.size + dayExercises.size

                                // Color map representing activity frequency (dark -> lower, light -> higher)
                                val cellColor = when {
                                    totalActivity == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    totalActivity == 1 -> Color(0xFF14532D) // Dark green
                                    totalActivity == 2 -> Color(0xFF16A34A) // Medium green
                                    totalActivity == 3 -> Color(0xFF4ADE80) // Bright/light green
                                    else -> Color(0xFF86EFAC) // Very bright/light green
                                }

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cellColor)
                                        .clickable {
                                            selectedDate = date
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mniej ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val intensities = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                Color(0xFF14532D),
                Color(0xFF16A34A),
                Color(0xFF4ADE80),
                Color(0xFF86EFAC)
            )
            intensities.forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
            Text(
                text = " Więcej",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Daily Details Dialog
    selectedDate?.let { date ->
        val dateStr = sdf.format(date)
        val dayMeasurements = measurementsGrouped[dateStr] ?: emptyList()
        val dayExercises = exercisesGrouped[dateStr] ?: emptyList()

        Dialog(onDismissRequest = { selectedDate = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = displayFormat.format(date).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (dayMeasurements.isEmpty() && dayExercises.isEmpty()) {
                        Text(
                            text = "Brak aktywności i pomiarów w tym dniu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        if (dayMeasurements.isNotEmpty()) {
                            Text(
                                text = "Pomiary medyczne:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            dayMeasurements.forEach { m ->
                                val detailText = when (m.type) {
                                    MeasurementType.BLOOD_PRESSURE -> 
                                        "${m.type.displayName}: ${m.value1.toInt()}/${m.value2?.toInt() ?: 0} ${m.type.unit}"
                                    else -> 
                                        "${m.type.displayName}: ${m.value1} ${m.type.unit}"
                                }
                                val notesSuffix = if (m.notes.isNotEmpty()) " (${m.notes})" else ""
                                Text(
                                    text = "• $detailText$notesSuffix",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                        }

                        if (dayExercises.isNotEmpty()) {
                            Text(
                                text = "Wykonane ćwiczenia / aktywności:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                            dayExercises.forEach { ex ->
                                val isWalk = ex.exerciseName.lowercase() == "spacer"
                                val detailText = if (isWalk) {
                                    val distText = if (ex.weight != null && ex.weight > 0.0) "${ex.weight} km" else ""
                                    val timeText = if (ex.reps > 0) "${ex.reps} min" else ""
                                    val stats = if (distText.isNotEmpty() && timeText.isNotEmpty()) "$distText • $timeText" else distText + timeText
                                    val cals = if (ex.calories > 0) " • ${ex.calories.toInt()} kcal" else ""
                                    "${ex.exerciseName}: $stats$cals"
                                } else {
                                    val unitText = if (ex.exerciseName.lowercase().contains("pompki") || ex.exerciseName.lowercase().contains("przysiady") || ex.exerciseName.lowercase().contains("mostki")) "powt." else "powt./sek."
                                    val weightText = if (ex.weight != null && ex.weight > 0.0) " + ${ex.weight} kg" else ""
                                    val cals = if (ex.calories > 0) " • ${ex.calories.toInt()} kcal" else ""
                                    "${ex.exerciseName}: ${ex.sets} serii x ${ex.reps} $unitText$weightText$cals"
                                }
                                val notesSuffix = if (ex.notes.isNotEmpty()) " (${ex.notes})" else ""
                                Text(
                                    text = "• $detailText$notesSuffix",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Zamknij")
                    }
                }
            }
        }
    }
}
