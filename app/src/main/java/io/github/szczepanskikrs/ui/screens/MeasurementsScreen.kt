package io.github.szczepanskikrs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import io.github.szczepanskikrs.data.MeasurementEntry
import io.github.szczepanskikrs.data.MeasurementType
import io.github.szczepanskikrs.ui.components.CustomChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val measurements by viewModel.measurements.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailType by remember { mutableStateOf<MeasurementType?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomiary Zdrowotne", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj Pomiar")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Dashboard cards
            item {
                Text(
                    text = "Kliknij na kartę, aby zobaczyć wykres trendu i historię.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Weight Card
            item {
                val weightLogs = measurements.filter { it.type == MeasurementType.WEIGHT }
                val latestWeight = weightLogs.firstOrNull()
                MeasurementCard(
                    title = "Waga",
                    latestValue = latestWeight?.let { "${it.value1} kg" } ?: "Brak wpisów",
                    date = latestWeight?.let { dateFormat.format(Date(it.timestamp)) } ?: "",
                    color = Color(0xFF6366F1),
                    icon = Icons.Default.MonitorWeight,
                    onClick = { selectedDetailType = MeasurementType.WEIGHT }
                )
            }

            // Blood Pressure Card
            item {
                val bpLogs = measurements.filter { it.type == MeasurementType.BLOOD_PRESSURE }
                val latestBp = bpLogs.firstOrNull()
                MeasurementCard(
                    title = "Ciśnienie krwi",
                    latestValue = latestBp?.let { "${it.value1.toInt()}/${it.value2?.toInt() ?: 0} mmHg" } ?: "Brak wpisów",
                    date = latestBp?.let { dateFormat.format(Date(it.timestamp)) } ?: "",
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.Favorite,
                    onClick = { selectedDetailType = MeasurementType.BLOOD_PRESSURE }
                )
            }

            // Blood Sugar Card
            item {
                val sugarLogs = measurements.filter { it.type == MeasurementType.BLOOD_SUGAR }
                val latestSugar = sugarLogs.firstOrNull()
                MeasurementCard(
                    title = "Cukier",
                    latestValue = latestSugar?.let { "${latestSugar.value1.toInt()} mg/dL" } ?: "Brak wpisów",
                    date = latestSugar?.let { dateFormat.format(Date(it.timestamp)) } ?: "",
                    color = Color(0xFF10B981),
                    icon = Icons.Default.Bloodtype,
                    onClick = { selectedDetailType = MeasurementType.BLOOD_SUGAR }
                )
            }
        }
    }

    // Measurement Add Dialog
    if (showAddDialog) {
        AddMeasurementDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, val1, val2, notes ->
                viewModel.addMeasurement(type, val1, val2, notes)
                showAddDialog = false
            }
        )
    }

    // Detail modal / Full sheet for trends and history
    selectedDetailType?.let { type ->
        val filteredLogs = measurements.filter { it.type == type }
        
        Dialog(onDismissRequest = { selectedDetailType = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trend: ${type.displayName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { selectedDetailType = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trend Line Chart
                    CustomChart(
                        entries = filteredLogs,
                        type = type,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Historia pomiarów",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Logs List
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Brak zarejestrowanych pomiarów.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredLogs) { log ->
                                HistoryRow(
                                    log = log,
                                    dateFormat = dateFormat,
                                    onDelete = { viewModel.deleteMeasurement(log.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementCard(
    title: String,
    latestValue: String,
    date: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = latestValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                if (date.isNotEmpty()) {
                    Text(
                        text = "Ostatni: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Szczegóły",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun HistoryRow(
    log: MeasurementEntry,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val (icon, color) = when (log.type) {
        MeasurementType.WEIGHT -> Pair(Icons.Default.MonitorWeight, Color(0xFF6366F1))
        MeasurementType.BLOOD_PRESSURE -> Pair(Icons.Default.Favorite, Color(0xFFEF4444))
        MeasurementType.BLOOD_SUGAR -> Pair(Icons.Default.Bloodtype, Color(0xFF10B981))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val valText = if (log.type == MeasurementType.BLOOD_PRESSURE) {
                    "${log.value1.toInt()}/${log.value2?.toInt() ?: 0} ${log.type.unit}"
                } else {
                    "${log.value1} ${log.type.unit}"
                }
                Text(
                    text = valText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.notes.isNotEmpty()) {
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (type: MeasurementType, val1: Double, val2: Double?, notes: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(MeasurementType.WEIGHT) }
    var value1Str by remember { mutableStateOf("") }
    var value2Str by remember { mutableStateOf("") } // Used only for BP (diastolic)
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dodaj Pomiar",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented buttons for type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MeasurementType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Button(
                            onClick = {
                                selectedType = type
                                value1Str = ""
                                value2Str = ""
                                errorMsg = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Input fields
                val label1 = when (selectedType) {
                    MeasurementType.WEIGHT -> "Waga (kg)"
                    MeasurementType.BLOOD_PRESSURE -> "Ciśnienie skurczowe (SYS)"
                    MeasurementType.BLOOD_SUGAR -> "Poziom cukru (mg/dL)"
                }

                OutlinedTextField(
                    value = value1Str,
                    onValueChange = {
                        value1Str = it
                        errorMsg = ""
                    },
                    label = { Text(label1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == MeasurementType.BLOOD_PRESSURE) {
                    OutlinedTextField(
                        value = value2Str,
                        onValueChange = {
                            value2Str = it
                            errorMsg = ""
                        },
                        label = { Text("Ciśnienie rozkurczowe (DIA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notatki (opcjonalnie)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v1 = value1Str.toDoubleOrNull()
                    if (v1 == null || v1 <= 0.0) {
                        errorMsg = "Wprowadź prawidłową wartość liczbową większą od zera."
                        return@Button
                    }

                    var v2: Double? = null
                    if (selectedType == MeasurementType.BLOOD_PRESSURE) {
                        v2 = value2Str.toDoubleOrNull()
                        if (v2 == null || v2 <= 0.0) {
                            errorMsg = "Wprowadź prawidłową wartość ciśnienia rozkurczowego."
                            return@Button
                        }
                    }

                    onSave(selectedType, v1, v2, notes)
                }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
