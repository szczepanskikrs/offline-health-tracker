package io.github.szczepanskikrs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import io.github.szczepanskikrs.ui.components.HeatmapGrid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val measurements by viewModel.measurements.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()

    // Calculate simple stats
    val totalWorkouts = exerciseLogs.size
    val totalMeasurements = measurements.size
    val totalCalories = remember(exerciseLogs) {
        exerciseLogs.sumOf { it.calories }.toInt()
    }
    
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val uniqueActiveDays = remember(measurements, exerciseLogs) {
        val dates = mutableSetOf<String>()
        measurements.forEach { dates.add(sdf.format(Date(it.timestamp))) }
        exerciseLogs.forEach { dates.add(sdf.format(Date(it.timestamp))) }
        dates.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heatmapa Aktywności", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Description card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Heatmapa obrazuje Twoją systematyczność. Każdy zielony kwadrat to dzień, w którym zanotowano pomiary lub treningi. Ciemniejszy odcień oznacza większą liczbę aktywności.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Heatmap Grid card container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    HeatmapGrid(
                        measurements = measurements,
                        exerciseLogs = exerciseLogs
                    )
                }
            }

            Text(
                text = "Statystyki Ogólne",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Stats grid layout (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Treningi",
                        value = "$totalWorkouts",
                        color = Color(0xFF6366F1),
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Pomiary",
                        value = "$totalMeasurements",
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.MonitorHeart,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Aktywne dni",
                        value = "$uniqueActiveDays",
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Spalone kalorie",
                        value = "$totalCalories kcal",
                        color = Color(0xFFE57373),
                        icon = Icons.Default.Whatshot,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
