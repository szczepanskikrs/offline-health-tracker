package io.github.szczepanskikrs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.szczepanskikrs.data.ExerciseLog
import io.github.szczepanskikrs.data.ExerciseType
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val exerciseTypes by viewModel.exerciseTypes.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()

    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var showAddTypeDialog by remember { mutableStateOf(false) }

    // Log Form State
    var repsStr by remember { mutableStateOf("") }
    var setsStr by remember { mutableStateOf("1") }
    var weightStr by remember { mutableStateOf("") }
    var caloriesStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    
    val isWalk = selectedExercise?.name?.lowercase() == "spacer"

    fun recalculateCalories(reps: String, sets: String, weight: String) {
        if (isWalk) {
            val distance = weight.replace(",", ".").toDoubleOrNull()
            val minutes = reps.toIntOrNull()
            if (distance != null && distance > 0.0) {
                caloriesStr = String.format(Locale.US, "%.1f", distance * 60.0)
            } else if (minutes != null && minutes > 0) {
                caloriesStr = String.format(Locale.US, "%.1f", minutes * 5.0)
            } else {
                caloriesStr = ""
            }
        } else {
            val repsVal = reps.toIntOrNull()
            val setsVal = sets.toIntOrNull()
            if (repsVal != null && setsVal != null && repsVal > 0 && setsVal > 0) {
                val baseKcal = selectedExercise?.caloriesPerRep ?: 0.4
                val w = weight.replace(",", ".").toDoubleOrNull() ?: 0.0
                val mult = 1.0 + w / 70.0
                caloriesStr = String.format(Locale.US, "%.1f", repsVal * setsVal * baseKcal * mult)
            } else {
                caloriesStr = ""
            }
        }
    }

    // Auto-select first exercise when list loads
    LaunchedEffect(exerciseTypes) {
        if (selectedExercise == null && exerciseTypes.isNotEmpty()) {
            selectedExercise = exerciseTypes.first()
        }
    }

    // Recalculate when selected exercise changes
    LaunchedEffect(selectedExercise) {
        if (isWalk) {
            setsStr = "1"
        }
        recalculateCalories(repsStr, setsStr, weightStr)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Śledzenie Ćwiczeń", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
            // Exercise selection header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wybierz ćwiczenie:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddTypeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj typ", fontSize = 14.sp)
                    }
                }
            }

            // Exercise Selection chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exerciseTypes.forEach { type ->
                        val isSelected = selectedExercise?.id == type.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedExercise = type },
                            label = { Text(type.name) },
                            trailingIcon = if (type.isCustom) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Usuń typ",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                viewModel.deleteExerciseType(type.id)
                                                if (selectedExercise?.id == type.id) {
                                                    selectedExercise = exerciseTypes.firstOrNull { it.id != type.id }
                                                }
                                            },
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Log session form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isWalk) "Zapisz spacer" else "Zapisz serię dla: ${selectedExercise?.name ?: "..."}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isWalk) {
                                OutlinedTextField(
                                    value = setsStr,
                                    onValueChange = {
                                        setsStr = it
                                        formError = ""
                                        recalculateCalories(repsStr, it, weightStr)
                                    },
                                    label = { Text("Serie") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = repsStr,
                                onValueChange = {
                                    repsStr = it
                                    formError = ""
                                    recalculateCalories(it, setsStr, weightStr)
                                },
                                label = { Text(if (isWalk) "Czas spaceru (min.)" else "Powtórzenia") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightStr,
                                onValueChange = {
                                    weightStr = it
                                    formError = ""
                                    recalculateCalories(repsStr, setsStr, it)
                                },
                                label = { Text(if (isWalk) "Dystans spaceru (km)" else "Ciężar (kg) - opcjonalnie") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = caloriesStr,
                                onValueChange = {},
                                label = { Text("Spalone kalorie (kcal)") },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isWalk) "Notatki / Trasa" else "Notatki") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (formError.isNotEmpty()) {
                            Text(
                                text = formError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val currentEx = selectedExercise
                                if (currentEx == null) {
                                    formError = "Wybierz ćwiczenie z listy powyżej."
                                    return@Button
                                }
                                val reps = repsStr.toIntOrNull()
                                val sets = if (isWalk) 1 else setsStr.toIntOrNull()
                                if (reps == null || reps <= 0 || sets == null || sets <= 0) {
                                    formError = if (isWalk) "Czas spaceru musi być dodatnią liczbą całkowitą." else "Liczba serii i powtórzeń musi być dodatnią liczbą całkowitą."
                                    return@Button
                                }
                                val weight = weightStr.replace(",", ".").toDoubleOrNull()
                                val calories = caloriesStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                                viewModel.addExerciseLog(
                                    exerciseId = currentEx.id,
                                    reps = reps,
                                    sets = sets,
                                    weight = weight,
                                    calories = calories,
                                    notes = notes
                                )

                                // Reset form values (except sets which defaults to 1)
                                repsStr = ""
                                if (!isWalk) {
                                    setsStr = "1"
                                }
                                weightStr = ""
                                caloriesStr = ""
                                notes = ""
                                formError = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val buttonIcon = if (isWalk) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Default.FitnessCenter
                            val buttonText = if (isWalk) "Zapisz spacer" else "Zapisz trening"
                            Icon(buttonIcon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(buttonText)
                        }
                    }
                }
            }

            // History header
            item {
                Text(
                    text = "Ostatnia historia treningów",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // History List
            if (exerciseLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak zarejestrowanych treningów. Czas zacząć ćwiczyć!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(exerciseLogs) { log ->
                    ExerciseLogCard(
                        log = log,
                        dateFormat = dateFormat,
                        onDelete = { viewModel.deleteExerciseLog(log.id) }
                    )
                }
            }
        }
    }

    // Add Custom Exercise Type Dialog
    if (showAddTypeDialog) {
        var newTypeName by remember { mutableStateOf("") }
        var newTypeCalories by remember { mutableStateOf("0.5") }
        var typeErrorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Dodaj Nowe Ćwiczenie", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Wpisz nazwę ćwiczenia oraz szacowaną liczbę spalanych kalorii za 1 powtórzenie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = {
                            newTypeName = it
                            typeErrorMsg = ""
                        },
                        label = { Text("Nazwa ćwiczenia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTypeCalories,
                        onValueChange = {
                            newTypeCalories = it
                            typeErrorMsg = ""
                        },
                        label = { Text("Kalorie za 1 powtórzenie (kcal)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (typeErrorMsg.isNotEmpty()) {
                        Text(text = typeErrorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = newTypeName.trim()
                        if (nameClean.isEmpty()) {
                            typeErrorMsg = "Nazwa nie może być pusta."
                            return@Button
                        }
                        if (exerciseTypes.any { it.name.equals(nameClean, ignoreCase = true) }) {
                            typeErrorMsg = "Ćwiczenie o tej nazwie już istnieje."
                            return@Button
                        }
                        val caloriesVal = newTypeCalories.replace(",", ".").toDoubleOrNull()
                        if (caloriesVal == null || caloriesVal < 0.0) {
                            typeErrorMsg = "Wpisz poprawną wartość kalorii (np. 0.5)."
                            return@Button
                        }
                        viewModel.addExerciseType(nameClean, caloriesVal)
                        showAddTypeDialog = false
                    }
                ) {
                    Text("Dodaj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTypeDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun ExerciseLogCard(
    log: ExerciseLog,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val isWalk = log.exerciseName.lowercase() == "spacer"
    val cardIcon = if (isWalk) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Default.SportsGymnastics
    val cardColor = if (isWalk) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

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
                    .background(cardColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = cardIcon,
                    contentDescription = null,
                    tint = cardColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val detailsText = if (isWalk) {
                    val distText = if (log.weight != null && log.weight > 0.0) "${log.weight} km" else ""
                    val timeText = if (log.reps > 0) "${log.reps} min" else ""
                    if (distText.isNotEmpty() && timeText.isNotEmpty()) {
                        "$distText • $timeText"
                    } else {
                        distText + timeText
                    }
                } else {
                    val repsLabel = if (log.exerciseName.lowercase().contains("pompki") || log.exerciseName.lowercase().contains("przysiady") || log.exerciseName.lowercase().contains("mostki")) "powtórzeń" else "powt./sek."
                    val weightText = if (log.weight != null && log.weight > 0.0) " + ${log.weight} kg" else ""
                    "${log.sets} serii x ${log.reps} $repsLabel$weightText"
                }

                val caloriesSuffix = if (log.calories > 0.0) {
                    val spacer = if (detailsText.isNotEmpty()) " • " else ""
                    "$spacer${log.calories.toInt()} kcal"
                } else ""

                Text(
                    text = detailsText + caloriesSuffix,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = cardColor
                )

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.notes.isNotEmpty()) {
                    Text(
                        text = if (isWalk) "Trasa: ${log.notes}" else "Komentarz: ${log.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń trening",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
